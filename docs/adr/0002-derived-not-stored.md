# Capability and visibility are derived, never stored

Docket stores facts — profile completeness, moderation actions, work-email verifications, the visibility dial, connections, blocks, positions — and never stores conclusions. Whether a Member may post a job, whether a Profile is visible to a given viewer, and whether a Thread may be written to are all computed from those facts at the point of asking.

A reasonable reader will expect `can_post_jobs` on the member record and be tempted to add it. Don't. Capability has three independent inputs — completeness ([Decide identity and signup](https://github.com/mandyMooreFan/docket/issues/7)), moderation withdrawals ([Spec moderation](https://github.com/mandyMooreFan/docket/issues/16)) and work verification plus a current position ([Decide how company entities are created and trusted](https://github.com/mandyMooreFan/docket/issues/14)) — and a stored flag is one missed update away from silently locking a member out of something they have earned.

## Consequences

- #16 requires that a Member can tell **never earned** apart from **withdrawn**. That falls out for free: they are different inputs, so the product can always say which one applies and why.
- Every access evaluates rules rather than reading a flag. This is the accepted cost.
- Questions like "list every suspended member" are queries over moderation actions, not over member flags.
- Reasoning and rejected alternatives: [Model the domain](https://github.com/mandyMooreFan/docket/issues/11). The map is the record.
