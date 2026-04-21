# Clarification Questions — TASK-20260318-393C91

## Q1 — Passenger accounts required?

**What sounded ambiguous:** Notification preferences, reservations, and missed check-ins imply persistent user state, but the prompt does not explicitly state that passengers must register or log in.

**How it was understood:** Passengers must register and log in to use notification preferences and reservations. Anonymous users can only perform route/stop searches.

**How it was solved:** Authentication is required for notification and reservation features. Search endpoints are publicly accessible without login.

---

## Q2 — Reservation flow scope

**What sounded ambiguous:** "Successful reservation" notifications are mentioned as a notification trigger event, but no reservation creation flow is described in the prompt.

**How it was understood:** Passengers can create a reservation for a bus trip/route. The reservation triggers notification events (successful reservation, upcoming reminder, missed check-in).

**How it was solved:** A basic reservation CRUD flow is implemented for passengers — create, view, and cancel reservations — which drives the notification event lifecycle.

---

## Q3 — Bus data source (HTML/JSON templates)

**What sounded ambiguous:** The prompt says the system supports "structured parsing of HTML/JSON templates" but does not specify whether these are uploaded manually or fetched from a network endpoint. The system is deployed in an offline LAN.

**How it was understood:** Given the offline LAN constraint, templates are manually uploaded by administrators. The system parses the uploaded files, maps fields, and tracks version changes.

**How it was solved:** An admin file-upload endpoint accepts HTML/JSON files. The parser extracts fields, applies cleaning rules, stores versioned records, and logs the source.

---

## Q4 — Queue backlog alert threshold

**What sounded ambiguous:** The prompt states "queue backlogs trigger local alerts" but defines no numeric threshold for what constitutes a backlog.

**How it was understood:** The threshold should be configurable by administrators, with a sensible default.

**How it was solved:** Default threshold is 100 unprocessed messages. The value is stored in the system configuration table and editable by administrators.

---

## Q5 — Pinyin matching on an English interface

**What sounded ambiguous:** The interface is described as English-language, but the prompt explicitly requires pinyin/initial letter matching for search. This implies bus stop names exist in Chinese in the database.

**How it was understood:** Stop names and related fields are stored in both Chinese and English. Pinyin indexing is applied to the Chinese name field to support pinyin/initial-letter search queries.

**How it was solved:** The stop entity stores a `name_cn` (Chinese), `name_en` (English), and `pinyin` (full pinyin) and `pinyin_initials` field. Search queries match across all four fields.
