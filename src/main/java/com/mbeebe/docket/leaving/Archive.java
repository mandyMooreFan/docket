package com.mbeebe.docket.leaving;

import com.mbeebe.docket.identity.Member;
import com.mbeebe.docket.images.Images;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * The one-button export (§11.1), built and streamed as a single ZIP.
 *
 * <p><strong>Why a ZIP, written straight to the response.</strong> §11.1 asks for
 * "one archive: JSON for portability plus readable pages", and images as well —
 * which is more than one file, so it is a container, and the container everybody's
 * computer already opens is a ZIP. It is built with {@code java.util.zip}: no new
 * dependency for a format the JDK has shipped since 1.1.
 *
 * <p><strong>Why synchronously.</strong> A job table, a worker, a stored artefact
 * and an expiring link is the shape this normally takes, and every piece of it is
 * a cost §11.1 does not ask for: the stored artefact is a file containing
 * everything about one person, sitting on disk, needing its own retention rule and
 * its own access control — a second copy of the very guard this route exists to
 * respect. Doing it in the request means there is never a second copy anywhere.
 * The accepted cost, stated plainly: one member's archive holds one request thread
 * while it builds, and the whole of it is assembled in memory before the first
 * byte is written. At v1's scale — one person's rows, images capped at 512KB each
 * (§10.4) — that is a small number of megabytes and a second or two. If archives
 * ever get big enough for that to hurt, the seam to move is this class, and the
 * route above it does not change.
 *
 * <p><strong>Images.</strong> Every image is fetched through {@code Images.serve}
 * with the departing member as the viewer, so the §51 audience guard decides what
 * goes in exactly as it decides what /images/{id} hands over. Everything in the
 * archive is something the member could already fetch by URL — their own photo,
 * their own Posts' images, images in Threads they are part of — so this is not a
 * way round the guard, and it cannot become one: an image the guard refuses is
 * simply absent, with no special case here to make it otherwise.
 */
@Service
class Archive {

    /**
     * §11.1's note, spec copy rather than a UI detail: WP242 says the whole Thread
     * goes to the subscriber, and the condition the guidance attaches is about what
     * the receiving side does with it. That condition, said readably, lives on
     * {@link ExportSection#CORRESPONDENCE_NOTE} — structurally, so a section
     * carrying somebody else's words cannot be built without it. It is repeated
     * here in three places, all of them before any of those words: the top of
     * README.txt, the top of the correspondence document, and above each
     * correspondence section.
     */
    static final String CORRESPONDENCE_NOTE = ExportSection.CORRESPONDENCE_NOTE;

    static final String JSON_FILE = "docket-export.json";
    static final String README_FILE = "README.txt";
    static final String OWN_FILE = "your-docket.html";
    static final String CORRESPONDENCE_FILE = "your-messages.html";
    static final String ABOUT_FILE = "about-this-copy.html";
    static final String MEDIA_DIR = "media/";

    private static final DateTimeFormatter WHEN =
            DateTimeFormatter.ofPattern("d MMMM uuuu, HH:mm", Locale.UK);

    private final ObjectProvider<ExportContributor> contributors;
    private final Images images;
    private final TemplateEngine templates;
    private final Clock clock;

    Archive(ObjectProvider<ExportContributor> contributors, Images images,
            TemplateEngine templates, Clock clock) {
        this.contributors = contributors;
        this.images = images;
        this.templates = templates;
        this.clock = clock;
    }

    /** One stored image, already through the audience guard, with its archive path. */
    private record Attachment(String path, byte[] bytes) {
    }

    /**
     * Writes this member's whole archive to the stream. Read-only from end to end:
     * taking a copy of your data changes nothing about it, and Art. 20(3) says
     * porting neither triggers nor delays erasure.
     */
    @Transactional(readOnly = true)
    void writeTo(Member member, OutputStream out) throws IOException {
        List<ExportSection> sections = contributors.orderedStream()
                .flatMap(contributor -> contributor.sectionsFor(member.id()).stream())
                .toList();

        Map<Long, Attachment> attachments = fetchImages(member, sections);
        List<ExportSection> resolved = sections.stream()
                .map(section -> section.with(resolveAll(section.records(), attachments)))
                .toList();
        List<ExportSection> own = resolved.stream()
                .filter(section -> section.kind() == ExportSection.Kind.OWN)
                .toList();
        List<ExportSection> correspondence = resolved.stream()
                .filter(section -> section.kind() == ExportSection.Kind.CORRESPONDENCE)
                .toList();

        String when = WHEN.format(clock.instant().atZone(clock.getZone()));
        try (ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
            write(zip, README_FILE, readme(when, own, correspondence));
            write(zip, OWN_FILE, page("export/own", "Your Docket", when, own, member));
            write(zip, CORRESPONDENCE_FILE,
                    page("export/correspondence", "Your messages", when, correspondence, member));
            write(zip, ABOUT_FILE, page("export/about", "About this copy", when, List.of(), member));
            write(zip, JSON_FILE, Json.write(document(member, when, resolved)));
            for (Attachment attachment : attachments.values()) {
                zip.putNextEntry(new ZipEntry(attachment.path()));
                zip.write(attachment.bytes());
                zip.closeEntry();
            }
        }
    }

    private String page(String template, String title, String when,
                        List<ExportSection> sections, Member member) {
        Context context = new Context(Locale.UK);
        context.setVariable("title", title);
        context.setVariable("generatedAt", when);
        context.setVariable("memberId", member.id());
        context.setVariable("sections", sections);
        context.setVariable("note", CORRESPONDENCE_NOTE);
        context.setVariable("supplementary", SupplementaryInformation.items());
        context.setVariable("ownFile", OWN_FILE);
        context.setVariable("correspondenceFile", CORRESPONDENCE_FILE);
        context.setVariable("aboutFile", ABOUT_FILE);
        context.setVariable("jsonFile", JSON_FILE);
        return templates.process(template, context);
    }

    /**
     * The first thing anyone sees when they unzip this, in plain text because a
     * plain text file opens everywhere and needs nothing. The note is its opening
     * line: §11.1 puts it "where a person will actually read it", and that is here
     * rather than three clicks into an HTML file.
     */
    private String readme(String when, List<ExportSection> own,
                          List<ExportSection> correspondence) {
        StringBuilder text = new StringBuilder();
        text.append("YOUR DOCKET DATA\n")
                .append("Taken on ").append(when).append("\n\n")
                .append(CORRESPONDENCE_NOTE).append("\n")
                .append("Your conversations are in here whole — what you wrote and what the\n")
                .append("other person wrote — because that is the only form in which a\n")
                .append("conversation makes sense. They are your copy to keep. They are not\n")
                .append("material to publish, to feed into anything, or to build a picture of\n")
                .append("someone who did not ask you to.\n\n")
                .append("WHAT IS IN HERE\n\n")
                .append("  ").append(OWN_FILE)
                .append("        Your profile and your writing, to read.\n")
                .append("  ").append(CORRESPONDENCE_FILE)
                .append("      Your conversations, whole, both sides.\n")
                .append("  ").append(ABOUT_FILE)
                .append("    What we use your data for, who else sees it,\n")
                .append("                          how long we keep it, and what is NOT in\n")
                .append("                          here and why. Worth reading.\n")
                .append("  ").append(JSON_FILE)
                .append("    The same thing again, for a machine.\n")
                .append("  ").append(MEDIA_DIR)
                .append("                 Your pictures.\n\n")
                .append("SECTIONS\n\n");
        for (ExportSection section : own) {
            text.append("  ").append(section.title()).append(" — ")
                    .append(count(section)).append("\n");
        }
        for (ExportSection section : correspondence) {
            text.append("  ").append(section.title()).append(" — ")
                    .append(count(section)).append("\n");
        }
        text.append("\nDeleting your account is a separate thing, and you never have to take\n")
                .append("this copy first.\n");
        return text.toString();
    }

    private static String count(ExportSection section) {
        int size = section.records().size();
        return size == 1 ? "1 entry" : size + " entries";
    }

    /**
     * The machine-readable half (§11.1, Art. 20's "structured, commonly used and
     * machine-readable"). Sections become keys and records become objects of their
     * own field names, rather than the label/value pairs the readable page wants —
     * the same facts, shaped for the thing that is going to read them.
     */
    private Map<String, Object> document(Member member, String when,
                                         List<ExportSection> sections) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("docket_export", "1");
        root.put("generated_at", when);
        root.put("member_id", member.id());
        root.put("note", CORRESPONDENCE_NOTE);
        Map<String, Object> supplementary = new LinkedHashMap<>();
        SupplementaryInformation.items()
                .forEach(item -> supplementary.put(item.key(), item.text()));
        root.put("supplementary_information", supplementary);
        Map<String, Object> body = new LinkedHashMap<>();
        for (ExportSection section : sections) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("title", section.title());
            node.put("records", section.records().stream().map(Archive::asMap).toList());
            body.put(section.key(), node);
        }
        root.put("sections", body);
        return root;
    }

    private static Map<String, Object> asMap(ExportRecord record) {
        Map<String, Object> node = new LinkedHashMap<>();
        if (record.label() != null && !record.label().isBlank()) {
            node.put("label", record.label());
        }
        record.fields().forEach(field -> node.put(field.key(), field.value()));
        if (!record.media().isEmpty()) {
            node.put("files", record.media().stream().map(ExportMedia::path)
                    .filter(java.util.Objects::nonNull).toList());
        }
        if (!record.entries().isEmpty()) {
            node.put("entries", record.entries().stream().map(Archive::asMap).toList());
        }
        return node;
    }

    /**
     * Every image any section asked for, fetched once, through the audience guard,
     * with the member as the viewer. An image the guard refuses simply never
     * arrives — no placeholder, no note, the same discipline /images/{id} applies.
     */
    private Map<Long, Attachment> fetchImages(Member member, List<ExportSection> sections) {
        Set<ExportMedia> wanted = new LinkedHashSet<>();
        sections.forEach(section -> collectMedia(section.records(), wanted));
        Map<Long, Attachment> found = new LinkedHashMap<>();
        for (ExportMedia media : wanted) {
            if (found.containsKey(media.imageId())) {
                continue;
            }
            images.serve(media.imageId(), Optional.of(member)).ifPresent(served -> found.put(
                    media.imageId(),
                    new Attachment(MEDIA_DIR + media.kind() + "-" + media.imageId()
                            + extensionFor(served.contentType()), served.data())));
        }
        return found;
    }

    private static void collectMedia(List<ExportRecord> records, Set<ExportMedia> into) {
        for (ExportRecord record : records) {
            into.addAll(record.media());
            collectMedia(record.entries(), into);
        }
    }

    private static List<ExportRecord> resolveAll(List<ExportRecord> records,
                                                 Map<Long, Attachment> attachments) {
        List<ExportRecord> resolved = new ArrayList<>(records.size());
        for (ExportRecord record : records) {
            List<ExportMedia> media = record.media().stream()
                    .filter(item -> attachments.containsKey(item.imageId()))
                    .map(item -> item.at(attachments.get(item.imageId()).path()))
                    .toList();
            resolved.add(record.withMedia(media, resolveAll(record.entries(), attachments)));
        }
        return resolved;
    }

    private static String extensionFor(String contentType) {
        return "image/png".equals(contentType) ? ".png" : ".jpg";
    }

    private static void write(ZipOutputStream zip, String name, String content) {
        try {
            zip.putNextEntry(new ZipEntry(name));
            zip.write(content.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        } catch (IOException failed) {
            throw new UncheckedIOException(failed);
        }
    }
}
