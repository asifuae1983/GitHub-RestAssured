package tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import listeners.RetryAnalyzer;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import pojo.RepositoryTestData;
import utils.Config;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Listeners({io.qameta.allure.testng.AllureTestNg.class, listeners.TestResultListener.class})
public class GetBranchDetailsTest {

    private RepositoryTestData testData;

    // Utility method for colored output: yellow for pass, red for fail
    private void printStatus(String msg, boolean isPass) {
        String color = isPass ? "\u001B[33m" : "\u001B[31m";
        System.out.println(color + msg + "\u001B[0m");
    }

    @BeforeClass
    public void setup() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        testData = mapper.readValue(new File("src/test/resources/testdata/TestData.json"), RepositoryTestData.class);
        RestAssured.baseURI = Config.getBaseUri();
    }

    /**
     * Test to list all branches in the repository.
     * Fetches the list of branches and prints/logs their names.
     */
    @Epic("GitHub Repository API")
    @Feature("Repository Branches")
    @Story("List Branches")
    @Description("Lists all branches in the repository using the GitHub API.")
    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void testListBranches() {
        try {
            Response response = given()
                .header("Authorization", "Bearer " + Config.getAuthToken())
                .header("Accept", "application/vnd.github+json")
            .when()
                .get("/repos/" + testData.getOwner() + "/" + testData.getRepo() + "/branches")
            .then()
                .statusCode(200)
                .body("name", notNullValue())
                .extract().response();

            List<String> branchNames = response.jsonPath().getList("name");
            String msg;
            if (branchNames == null || branchNames.isEmpty()) {
                msg = "Test failed: No branches found in the repository.";
                printStatus(msg, false);
                Allure.step(msg);
                Assert.fail(msg);
            } else {
                msg = "Test passed: testListBranches. Branches: " + branchNames;
                printStatus(msg, true);
                Allure.step(msg);
            }
        } catch (Exception e) {
            String msg = "Test failed: testListBranches";
            printStatus(msg, false);
            e.printStackTrace();
            Allure.step("Exception in testListBranches: " + e.getMessage());
            assert false : "Exception in testGetBranch: " + e.getMessage();
        }
    }

    /**
     * Test to get details of a specific branch in the repository.
     * Fetches the branch details and prints/logs the branch name.
     */
    @Epic("GitHub Repository API")
    @Feature("Repository Branches")
    @Story("Get Branch")
    @Description("Gets details of a specific branch in the repository using the GitHub API.")
    @Test(priority = 16, dataProvider = "branchProvider", retryAnalyzer = RetryAnalyzer.class)
    public void testGetBranch(String branch) {
        try {
            Response response = given()
                .header("Authorization", "Bearer " + Config.getAuthToken())
                .header("Accept", "application/vnd.github+json")
            .when()
                .get("/repos/" + testData.getOwner() + "/" + testData.getRepo() + "/branches/" + branch)
            .then()
                .statusCode(200)
                .body("name", equalTo(branch))
                .extract().response();

            String msg = "Test passed: testGetBranch. Branch details fetched for: " + branch;
            printStatus(msg, true);
            Allure.step(msg);
        } catch (Exception e) {
            String msg = "Test failed: testGetBranch";
            printStatus(msg, false);
            e.printStackTrace();
            Allure.step("Exception in testGetBranch: " + e.getMessage());
            assert false : "Exception in testGetBranch: " + e.getMessage();
        }
    }

    @DataProvider(name = "branchProvider")
    public Object[][] branchProvider() {
        return new Object[][] {
            {"master"},
            {"develop"}
            // Add more branch names as needed
        };
    }
}