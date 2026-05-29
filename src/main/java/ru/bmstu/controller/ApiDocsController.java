package ru.bmstu.controller;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApiDocsController {

    private final OpenAPI openAPI;

    public ApiDocsController(OpenAPI openAPI) {
        this.openAPI = openAPI;
    }

    @GetMapping("/v3/api-docs")
    public ResponseEntity<OpenAPI> getApiDocs() {
        openAPI.setPaths(buildPaths());
        return ResponseEntity.ok(openAPI);
    }

    private Paths buildPaths() {
        Paths paths = new Paths();

        paths.addPathItem("/api/v1/status", new PathItem()
                .get(new Operation()
                        .summary("Service status")
                        .addTagsItem("Status")
                        .responses(ok200("UP status"))));

        paths.addPathItem("/api/v1/creatures", new PathItem()
                .get(new Operation()
                        .summary("List available creatures")
                        .addTagsItem("Creatures")
                        .responses(ok200("List of available creatures"))));

        paths.addPathItem("/api/v1/creatures/all", new PathItem()
                .get(new Operation()
                        .summary("List all creatures (ADMIN only)")
                        .addTagsItem("Creatures")
                        .addParametersItem(xRoleHeader())
                        .responses(ok200AndForbidden())));

        paths.addPathItem("/api/v1/creatures/{id}", new PathItem()
                .get(new Operation()
                        .summary("Get creature by ID")
                        .addTagsItem("Creatures")
                        .addParametersItem(pathParam("id", "Creature ID"))
                        .responses(ok200("Creature").addApiResponse("404", new ApiResponse().description("Not found")))));

        paths.addPathItem("/api/v1/creatures/search", new PathItem()
                .get(new Operation()
                        .summary("Search creatures by name")
                        .addTagsItem("Creatures")
                        .addParametersItem(queryParam("name", "Name substring (case-insensitive)"))
                        .responses(ok200("Matching creatures"))));

        paths.addPathItem("/api/v1/creatures/filter", new PathItem()
                .get(new Operation()
                        .summary("Filter by species or temperament")
                        .addTagsItem("Creatures")
                        .addParametersItem(queryParam("species", "Species name").required(false))
                        .addParametersItem(queryParam("temperament", "Temperament").required(false))
                        .responses(ok200("Filtered creatures"))));

        paths.addPathItem("/api/v1/adoptions", new PathItem()
                .post(new Operation()
                        .summary("Adopt a creature")
                        .addTagsItem("Adoptions")
                        .requestBody(new RequestBody()
                                .required(true)
                                .content(new Content().addMediaType("application/json",
                                        new MediaType().schema(new Schema<>()
                                                .addProperty("visitorName", new Schema<>().type("string"))
                                                .addProperty("monthlyBudget", new Schema<>().type("number"))
                                                .addProperty("creatureId", new Schema<>().type("string"))))))
                        .responses(ok200("Adoption contract")
                                .addApiResponse("400", new ApiResponse().description("Budget insufficient"))
                                .addApiResponse("404", new ApiResponse().description("Creature not found")))));

        paths.addPathItem("/api/v1/statistics", new PathItem()
                .get(new Operation()
                        .summary("Shelter statistics")
                        .addTagsItem("Statistics")
                        .responses(ok200("Statistics"))));

        paths.addPathItem("/api/v1/audit", new PathItem()
                .get(new Operation()
                        .summary("Audit log (ADMIN only)")
                        .addTagsItem("Statistics")
                        .addParametersItem(xRoleHeader())
                        .responses(ok200AndForbidden())));

        return paths;
    }

    private ApiResponses ok200(String description) {
        return new ApiResponses()
                .addApiResponse("200", new ApiResponse().description(description));
    }

    private ApiResponses ok200AndForbidden() {
        return ok200("Success")
                .addApiResponse("403", new ApiResponse().description("Access denied: requires role ADMIN"));
    }

    private Parameter pathParam(String name, String description) {
        return new Parameter().name(name).in("path").required(true)
                .description(description).schema(new Schema<>().type("string"));
    }

    private Parameter queryParam(String name, String description) {
        return new Parameter().name(name).in("query").required(true)
                .description(description).schema(new Schema<>().type("string"));
    }

    private Parameter xRoleHeader() {
        return new Parameter().name("X-Role").in("header").required(false)
                .description("Role: USER or ADMIN (default: USER)")
                .schema(new Schema<>().type("string")._enum(java.util.List.of("USER", "ADMIN")));
    }
}
