package ru.bmstu.controller;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.Components;
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
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class ApiDocsController {

    private final OpenAPI baseOpenAPI;

    public ApiDocsController(OpenAPI baseOpenAPI) {
        this.baseOpenAPI = baseOpenAPI;
    }

    @GetMapping("/v3/api-docs")
    public void getApiDocs(HttpServletResponse response) throws IOException {
        // Components строим здесь напрямую — обходим проблему сериализации
        // нашим кастомным ObjectMapper (он не знает о swagger-core аннотациях)
        Components components = new Components()
                .addSecuritySchemes("ApiKeyAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name("X-Api-Key")
                        .description("Use POST /auth/login to get your API key"));

        // Новый объект каждый раз — не мутируем синглтон baseOpenAPI
        OpenAPI spec = new OpenAPI()
                .info(baseOpenAPI.getInfo())
                .components(components)
                .paths(buildPaths());

        // Json.mapper() из swagger-core правильно сериализует все OpenAPI-модели
        response.setContentType("application/json;charset=UTF-8");
        Json.mapper().writeValue(response.getOutputStream(), spec);
    }

    private Paths buildPaths() {
        Paths paths = new Paths();

        paths.addPathItem("/auth/login", new PathItem()
                .post(new Operation()
                        .summary("Login — get API key by username and password")
                        .addTagsItem("Auth")
                        .requestBody(loginBody())
                        .responses(ok("API key and role")
                                .addApiResponse("401", new ApiResponse()
                                        .description("Invalid credentials")))));

        paths.addPathItem("/api/v1/status", new PathItem()
                .get(op("Service status", "Status", ok("Status response"))));

        paths.addPathItem("/api/v1/creatures", new PathItem()
                .get(op("List available (not adopted) creatures", "Creatures", ok("List of creatures"))));

        paths.addPathItem("/api/v1/creatures/all", new PathItem()
                .get(adminOp("List all creatures including adopted — ADMIN only", "Creatures",
                        ok("All creatures")
                                .addApiResponse("401", unauthorized())
                                .addApiResponse("403", denied()))));

        paths.addPathItem("/api/v1/creatures/{id}", new PathItem()
                .get(op("Get creature by ID", "Creatures",
                        ok("Creature").addApiResponse("404", new ApiResponse().description("Not found")))
                        .addParametersItem(path("id", "Creature ID"))));

        paths.addPathItem("/api/v1/creatures/search", new PathItem()
                .get(op("Search by name (case-insensitive)", "Creatures", ok("Matching creatures"))
                        .addParametersItem(query("name", "Name substring"))));

        paths.addPathItem("/api/v1/creatures/filter", new PathItem()
                .get(op("Filter by species or temperament", "Creatures", ok("Filtered creatures"))
                        .addParametersItem(query("species", "Species name").required(false))
                        .addParametersItem(query("temperament", "Temperament").required(false))));

        paths.addPathItem("/api/v1/adoptions", new PathItem()
                .post(new Operation()
                        .summary("Adopt a creature")
                        .addTagsItem("Adoptions")
                        .requestBody(adoptionBody())
                        .responses(ok("Adoption contract")
                                .addApiResponse("400", new ApiResponse().description("Budget insufficient — returns alternatives"))
                                .addApiResponse("404", new ApiResponse().description("Creature not found")))));

        paths.addPathItem("/api/v1/statistics", new PathItem()
                .get(op("Shelter statistics", "Statistics", ok("Statistics object"))));

        paths.addPathItem("/api/v1/audit", new PathItem()
                .get(adminOp("Audit log — ADMIN only", "Statistics",
                        ok("Log entries")
                                .addApiResponse("401", unauthorized())
                                .addApiResponse("403", denied()))));

        return paths;
    }

    private Operation op(String summary, String tag, ApiResponses responses) {
        return new Operation().summary(summary).addTagsItem(tag).responses(responses);
    }

    /** Операция, требующая ADMIN API-ключа — вешает замочек в Swagger. */
    private Operation adminOp(String summary, String tag, ApiResponses responses) {
        return op(summary, tag, responses)
                .addSecurityItem(new SecurityRequirement().addList("ApiKeyAuth"));
    }

    private ApiResponses ok(String description) {
        return new ApiResponses().addApiResponse("200", new ApiResponse().description(description));
    }

    private ApiResponse denied() {
        return new ApiResponse().description("Access denied: requires ADMIN role");
    }

    private ApiResponse unauthorized() {
        return new ApiResponse().description("API key missing or invalid");
    }

    private Parameter path(String name, String description) {
        return new Parameter().name(name).in("path").required(true)
                .description(description)
                .schema(new Schema<String>().type("string"));
    }

    private Parameter query(String name, String description) {
        return new Parameter().name(name).in("query").required(true)
                .description(description)
                .schema(new Schema<String>().type("string"));
    }

    private RequestBody adoptionBody() {
        return new RequestBody().required(true)
                .content(new Content().addMediaType("application/json",
                        new MediaType().schema(new Schema<>()
                                .type("object")
                                .addProperty("visitorName", new Schema<String>().type("string").example("Alex"))
                                .addProperty("monthlyBudget", new Schema<Number>().type("number").example(300.0))
                                .addProperty("creatureId", new Schema<String>().type("string").example("1")))));
    }

    private RequestBody loginBody() {
        return new RequestBody().required(true)
                .content(new Content().addMediaType("application/json",
                        new MediaType().schema(new Schema<>()
                                .type("object")
                                .addProperty("username", new Schema<String>().type("string").example("admin"))
                                .addProperty("password", new Schema<String>().type("string").example("admin123")))));
    }
}
