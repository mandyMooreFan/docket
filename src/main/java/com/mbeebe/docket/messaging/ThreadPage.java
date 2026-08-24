package com.mbeebe.docket.messaging;

import com.mbeebe.docket.profile.PersonCard;

import java.util.List;

/**
 * One Thread as one participant sees it (§7.2). {@code mayWrite} is derived at
 * every ask (ADR-0002), never stored, so a Disconnect, a Block, a Block lifted
 * or a Connection remade needs no write to the Thread at all.
 *
 * <p>{@code note} is what replaces the composer when {@code mayWrite} is false,
 * and it is deliberately the same sentence whether the Thread closed because
 * the Connection ended or because of a Block (§7.3). §4.2's silent-decline
 * discipline applied to endings: a Block a person can detect is a Block that
 * tells them something, and that is the whole thing §4.2 refuses to do.
 */
public record ThreadPage(long otherId, PersonCard other, List<MessageView> messages,
                         boolean mayWrite, String note) {
}
