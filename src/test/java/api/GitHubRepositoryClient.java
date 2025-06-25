package api;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

public class GitHubRepositoryClient {
    private final RequestSpecification spec;

    public GitHubRepositoryClient(RequestSpecification spec) {
        this.spec = spec;
    }

    public Response getRepository() {
        return spec.when().get();
    }

    public Response updateRepository(Object updateBody, String authToken) {
        return spec.header("Authorization", "Bearer " + authToken)
                   .body(updateBody)
                   .when().patch();
    }

    public Response listEvents() {
        return spec.when().get("/events");
    }

    public Response listContributors() {
        return spec.when().get("/contributors");
    }

    public Response checkDependabot(String authToken) {
        return spec.header("Authorization", "Bearer " + authToken)
                   .when().get("/automated-security-fixes");
    }

    public Response deleteRepository(String authToken) {
        return spec.header("Authorization", "Bearer " + authToken)
                   .when().delete();
    }
}