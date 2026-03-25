package com.anju.integration;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;

import java.time.LocalDate;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ApiFunctionalTest {

    private static String adminUsername;
    private static final String ADMIN_PASSWORD = "Admin1234";
    private static final String ADMIN_SECONDARY_PASSWORD = "Admin5678";
    private static String staffUsername;
    private static final String STAFF_PASSWORD = "Staff1234";

    @BeforeAll
    public static void setup() {
        String basePath = System.getenv("API_BASE_URL");
        if (basePath != null && !basePath.isEmpty()) {
            RestAssured.baseURI = basePath;
        } else {
            RestAssured.baseURI = "http://localhost";
        }
        RestAssured.port = 8080;
        adminUsername = "admin_api_" + System.currentTimeMillis();
        staffUsername = "staff_" + System.currentTimeMillis();

        String adminRegisterPayload = "{" +
                "\"username\":\"" + adminUsername + "\"," +
                "\"password\":\"" + ADMIN_PASSWORD + "\"," +
                "\"displayName\":\"API Test Admin\"," +
            "\"role\":\"ADMIN\"," +
            "\"secondaryPassword\":\"" + ADMIN_SECONDARY_PASSWORD + "\"" +
                "}";

        given()
            .contentType(ContentType.JSON)
            .body(adminRegisterPayload)
            .when()
                .post("/auth/register")
            .then()
                .statusCode(anyOf(is(200), is(409)));
    }

    @Test
    @Order(1)
    public void testGetProperties_requiresAuthentication() {
        given()
            .when()
                .get("/api/properties")
            .then()
                .statusCode(401);
    }

    @Test
    @Order(2)
    public void testGetProperties_withAdminBasicAuth_shouldReturn200() {
        given()
            .auth().preemptive().basic(adminUsername, ADMIN_PASSWORD)
            .when()
                .get("/api/properties")
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("code", equalTo(0));
    }

    @Test
    @Order(3)
    public void testCreateAppointment_missingFields_shouldReturn400() {
        String payload = "{ \"staffId\": 1 }"; 
        given()
            .auth().preemptive().basic(adminUsername, ADMIN_PASSWORD)
            .contentType(ContentType.JSON)
            .body(payload)
            .when()
                .post("/appointment")
            .then()
                .statusCode(400)
                .contentType(ContentType.JSON);
    }

    @Test
    @Order(4)
    public void testFinanceAccess_forbiddenForStaffRole() {
        String registerPayload = "{" +
                "\"username\":\"" + staffUsername + "\"," +
                "\"password\":\"" + STAFF_PASSWORD + "\"," +
                "\"displayName\":\"API Test Staff\"," +
                "\"role\":\"STAFF\"" +
                "}";

        given()
            .contentType(ContentType.JSON)
            .body(registerPayload)
            .when()
                .post("/auth/register")
            .then()
                .statusCode(anyOf(is(200), is(409)));

        given()
            .auth().preemptive().basic(staffUsername, STAFF_PASSWORD)
            .when()
                .get("/finance")
            .then()
                .statusCode(403);
    }

    @Test
    @Order(5)
    public void testFinanceBookkeeping_idempotencyReplayReturnsSameTransaction() {
        String idempotencyKey = "idem-fin-" + System.currentTimeMillis();
        String transactionNo = "TX-IDEM-" + System.currentTimeMillis();

        String payload = "{" +
                "\"transactionNumber\":\"" + transactionNo + "\"," +
                "\"amount\":120.50," +
                "\"type\":\"PAYMENT\"," +
                "\"currency\":\"USD\"," +
                "\"remark\":\"idem-check\"" +
                "}";

        Response first = given()
                .auth().preemptive().basic(adminUsername, ADMIN_PASSWORD)
                .header("X-Idempotency-Key", idempotencyKey)
                .contentType(ContentType.JSON)
                .body(payload)
            .when()
                .post("/finance/bookkeeping")
            .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .extract().response();

        String firstTransactionNo = first.jsonPath().getString("data.transactionNo");

        Response second = given()
                .auth().preemptive().basic(adminUsername, ADMIN_PASSWORD)
                .header("X-Idempotency-Key", idempotencyKey)
                .contentType(ContentType.JSON)
                .body(payload)
            .when()
                .post("/finance/bookkeeping")
            .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .extract().response();

        second.then().body("data.transactionNo", equalTo(firstTransactionNo));
    }

    @Test
    @Order(6)
    public void testFinanceExceptionAndStatementExportFlows() {
        String transactionNo = "TX-EXC-" + System.currentTimeMillis();

        String createPayload = "{" +
                "\"transactionNumber\":\"" + transactionNo + "\"," +
                "\"amount\":88.25," +
                "\"type\":\"PAYMENT\"," +
                "\"currency\":\"USD\"," +
                "\"remark\":\"exception-flow\"" +
                "}";

        given()
            .auth().preemptive().basic(adminUsername, ADMIN_PASSWORD)
            .contentType(ContentType.JSON)
            .body(createPayload)
        .when()
            .post("/finance/bookkeeping")
        .then()
            .statusCode(200)
            .body("code", equalTo(0));

        String markExceptionPayload = "{" +
                "\"reason\":\"manual reconciliation mismatch\"" +
                "}";

        given()
            .auth().preemptive().basic(adminUsername, ADMIN_PASSWORD)
            .contentType(ContentType.JSON)
            .body(markExceptionPayload)
        .when()
            .post("/finance/" + transactionNo + "/exception")
        .then()
            .statusCode(200)
            .body("code", equalTo(0))
            .body("data.exceptionFlag", equalTo(true));

        String today = LocalDate.now().toString();
        given()
            .auth().preemptive().basic(adminUsername, ADMIN_PASSWORD)
            .queryParam("date", today)
        .when()
            .get("/finance/statements/daily")
        .then()
            .statusCode(200)
            .body("code", equalTo(0))
            .body("data.exceptionCount", greaterThanOrEqualTo(1));

        given()
            .auth().preemptive().basic(adminUsername, ADMIN_PASSWORD)
            .queryParam("date", today)
        .when()
            .get("/finance/statements/daily/export")
        .then()
            .statusCode(200)
            .header("Content-Disposition", containsString("statement-" + today + ".csv"))
            .body(containsString("exceptionCount"));
    }

    @Test
    @Order(7)
    public void testAppointmentGetById_notFoundReturns404() {
        given()
            .auth().preemptive().basic(adminUsername, ADMIN_PASSWORD)
        .when()
            .get("/appointment/999999999")
        .then()
            .statusCode(404)
            .body("code", equalTo(4042));
    }

    @Test
    @Order(8)
    public void testFinanceGetByTransactionNo_notFoundReturns404() {
        given()
            .auth().preemptive().basic(adminUsername, ADMIN_PASSWORD)
        .when()
            .get("/finance/TX-NOT-FOUND")
        .then()
            .statusCode(404)
            .body("code", equalTo(4040));
    }

    @Test
    @Order(9)
    public void testFinanceMarkException_repeatReturns409() {
        String requestTransactionNo = "TX-EXC-REPEAT-" + System.currentTimeMillis();
        String createPayload = "{" +
                "\"transactionNumber\":\"" + requestTransactionNo + "\"," +
                "\"amount\":55.00," +
                "\"type\":\"PAYMENT\"," +
                "\"currency\":\"USD\"," +
                "\"remark\":\"repeat-exception\"" +
                "}";

        Response createResponse = given()
            .auth().preemptive().basic(adminUsername, ADMIN_PASSWORD)
            .contentType(ContentType.JSON)
            .body(createPayload)
        .when()
            .post("/finance/bookkeeping")
        .then()
            .statusCode(200)
            .body("code", equalTo(0))
            .extract().response();

        String transactionNo = createResponse.jsonPath().getString("data.transactionNo");

        String exceptionPayload = "{" +
                "\"reason\":\"first mark\"" +
                "}";

        given()
            .auth().preemptive().basic(adminUsername, ADMIN_PASSWORD)
            .contentType(ContentType.JSON)
            .body(exceptionPayload)
        .when()
            .post("/finance/" + transactionNo + "/exception")
        .then()
            .statusCode(200)
            .body("code", equalTo(0));

        given()
            .auth().preemptive().basic(adminUsername, ADMIN_PASSWORD)
            .contentType(ContentType.JSON)
            .body(exceptionPayload)
        .when()
            .post("/finance/" + transactionNo + "/exception")
        .then()
            .statusCode(409)
            .body("code", equalTo(4092));
    }

    @Test
    @Order(10)
    public void testFileOwnership_idorReturns403() {
        String attackerUsername = "staff_idor_" + System.currentTimeMillis();
        String registerPayload = "{" +
                "\"username\":\"" + attackerUsername + "\"," +
                "\"password\":\"" + STAFF_PASSWORD + "\"," +
                "\"displayName\":\"IDOR Staff\"," +
                "\"role\":\"STAFF\"" +
                "}";

        given()
            .contentType(ContentType.JSON)
            .body(registerPayload)
        .when()
            .post("/auth/register")
        .then()
            .statusCode(anyOf(is(200), is(409)));

        String hash = "idorhash" + System.currentTimeMillis();
        String uploadPayload = "{" +
                "\"hash\":\"" + hash + "\"," +
                "\"fileName\":\"proof.txt\"," +
                "\"contentType\":\"text/plain\"," +
                "\"sizeBytes\":12," +
                "\"chunks\":1," +
                "\"currentChunk\":1" +
                "}";

        Response uploadResponse = given()
            .auth().preemptive().basic(adminUsername, ADMIN_PASSWORD)
            .contentType(ContentType.JSON)
            .body(uploadPayload)
        .when()
            .post("/file/upload")
        .then()
            .statusCode(200)
            .body("code", equalTo(0))
            .extract().response();

        Long fileId = uploadResponse.jsonPath().getLong("data.id");

        given()
            .auth().preemptive().basic(attackerUsername, STAFF_PASSWORD)
        .when()
            .get("/file/" + fileId)
        .then()
            .statusCode(403)
            .body("code", equalTo(4034));
    }
}
