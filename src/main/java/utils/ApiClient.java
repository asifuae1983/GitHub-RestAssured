package utils;

import io.restassured.response.Response;
import io.restassured.RestAssured;
import java.util.Map;

public class ApiClient {
    private static final String BASE_URL = "https://api.example.com"; // Replace with actual API base URL

    public ApiClient() {
    }

    public Response get(String endpoint, Map<String, String> queryParams) {
        return RestAssured.given().queryParams(queryParams).get(BASE_URL + endpoint);
    }

    public Response post(String endpoint, Object body, Map<String, String> headers) {
        return RestAssured.given().headers(headers).body(body).post(BASE_URL + endpoint);
    }

    public Response put(String endpoint, Object body, Map<String, String> headers) {
        return RestAssured.given().headers(headers).body(body).put(BASE_URL + endpoint);
    }

    public Response delete(String endpoint, Map<String, String> headers) {
        return RestAssured.given().headers(headers).delete(BASE_URL + endpoint);
    }
}
