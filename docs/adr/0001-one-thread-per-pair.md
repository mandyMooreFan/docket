# One Thread per pair, authorised by a Connection or an open Application

Two Members have exactly one Thread between them, ever, and it is permanent. What changes is not the Thread but whether writing to it is currently authorised: being connected authorises it, and an open job Application between the two also authorises it. When the authorisation ends the Thread closes and its history is kept.

This is deliberately not two mechanisms. [Spec messaging](https://github.com/mandyMooreFan/docket/issues/6) requires one gate and one unread count — the unread count is the only badge in the product — while [Spec the jobs board](https://github.com/mandyMooreFan/docket/issues/5) requires a poster to be able to reply to an applicant and to nobody else. Modelling the application channel separately would have produced a second inbox and a second unread count.

## Consequences

- "Why can I write to this person?" is a derived answer, not a stored flag. See ADR-0002.
- If a poster and an applicant later connect, the same correspondence simply continues; there is no second thread to reconcile.
- Reasoning and rejected alternatives: [Model the domain](https://github.com/mandyMooreFan/docket/issues/11) and [Spec messaging](https://github.com/mandyMooreFan/docket/issues/6). The map is the record.
