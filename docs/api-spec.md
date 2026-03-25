# API Specifications

All endpoints return a standardized `Result<T>` JSON response format:
```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```
Errors return `code: 400` (or appropriate HTTP status mapping) with an error `message`.

## Authentication API

### 1. Register
**Endpoint:** `POST /auth/register`
**Description:** Registers a user with a role and password policy enforcement.

### 2. Login
**Endpoint:** `POST /auth/login`
**Description:** Validates credentials and updates last-login timestamp. Protected routes use HTTP Basic auth.

### 3. Verify Secondary Password
**Endpoint:** `POST /auth/verify-secondary`
**Description:** Validates the secondary password required by sensitive operations.

---

## Property API

### 1. Create Property
**Endpoint:** `POST /api/properties`
**Description:** Creates a new property listing.
**Request Body (`CreatePropertyRequest`):**
```json
{
  "code": "PROP-1001",
  "status": "AVAILABLE",
  "rent": 1500.00,
  "deposit": 3000.00,
  "startDate": "2024-01-01T00:00:00",
  "endDate": "2025-01-01T00:00:00",
  "complianceStatus": "PENDING"
}
```
**Response Body:** Returns the created `PropertyResponse` wrapped in `Result<T>`.

### 2. Compliance Review
**Endpoint:** `PATCH /api/properties/{id}/compliance-review`
**Description:** Updates the compliance status of a property (requires role and secondary password).
**Request Body (`ComplianceReviewRequest`):**
```json
{
  "propertyId": 1,
  "complianceStatus": "APPROVED",
  "reviewerNotes": "Passed all checks."
}
```

---

## Appointment API

### 1. Create Appointment
**Endpoint:** `POST /appointment`
**Description:** Books a new medical appointment, validating against scheduling conflicts for the staff and resource.
**Request Body (`CreateAppointmentRequest`):**
```json
{
  "staffId": 101,
  "resourceId": 201,
  "startTime": "2024-05-10T10:00:00",
  "endTime": "2024-05-10T11:00:00",
  "status": "SCHEDULED",
  "penalty": 0.0,
  "rescheduleCount": 0
}
```
**Response Body:** Returns the generated `AppointmentResponse` wrapped in `Result<T>`.

### 2. Reschedule Appointment
**Endpoint:** `PUT /appointment/reschedule`
**Description:** Reschedules an existing appointment. Enforces maximum 2 reschedules and 24-hour advance notice rules.
**Request Body (`RescheduleAppointmentRequest`):**
```json
{
  "appointmentId": 1,
  "startTime": "2024-05-12T10:00:00",
  "endTime": "2024-05-12T11:00:00"
}
```

### 3. Cancel Appointment
**Endpoint:** `PUT /appointment/{id}/cancel`
**Description:** Cancels appointment and applies late-cancel penalty cap when within 24 hours.

---

## Finance API

### 1. Record Transaction
**Endpoint:** `POST /finance`
**Description:** Records a transaction (requires finance role and secondary password).

### 2. Refund
**Endpoint:** `POST /finance/{transactionNo}/refund`
**Description:** Creates a linked refund with amount guardrails.

### 3. Daily Settlement
**Endpoint:** `POST /finance/settlements/daily`
**Description:** Settles successful daily payment transactions.

### 4. Daily Statement
**Endpoint:** `GET /finance/statements/daily?date=YYYY-MM-DD`
**Description:** Returns payment/refund/net summary for the day.

### 5. Invoice Lifecycle
**Endpoints:**
- `POST /finance/{transactionNo}/invoice/request`
- `POST /finance/{transactionNo}/invoice/issue`

---

## File API

### 1. Resumable Upload and Fast Upload
**Endpoints:**
- `GET /file/check?hash=...`
- `POST /file/upload`

### 2. Lifecycle Management
**Endpoints:**
- `GET /file`
- `GET /file/{id}`
- `DELETE /file/{id}`
- `POST /file/{id}/restore`
- `GET /file/recycle-bin`
- `POST /file/{hash}/rollback/{targetVersion}`

### 3. Preview Flow
**Endpoints:**
- `GET /file/{id}/preview`
- `GET /file/{id}/content`
