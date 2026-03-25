package com.anju.integration;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class ApiFunctionalTest {

    private static String adminUsername;
    private static final String ADMIN_PASSWORD = "Admin1234";
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
                "\"role\":\"ADMIN\"" +
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
}
