# Typed Group Polls only (no free-form polls)

Group Polls are restricted to two typed variants: Destination Poll (candidates are Trip Destination options from Level 1) and Activity Poll (candidates are Place Recommendations from Level 2). Free-form polls — where the ADMIN types an arbitrary question — are not supported.

Free-form polls were considered. They would cover cases like "¿Viajamos en verano o invierno?" or "¿Alquilamos auto?". They were rejected because the app cannot take an automated action on a free-form winner; the ADMIN would have to act manually, reducing the poll to a chat-tool feature. Typed polls close the loop: the winner of a Destination Poll sets the Trip Destination; the winner of an Activity Poll adds an Itinerary Event. That automated outcome is the product value of the polling feature.

Free-form polls can be reconsidered in v2 if there is user demand, without breaking the typed poll data model (the two types would coexist as distinct poll kinds).
