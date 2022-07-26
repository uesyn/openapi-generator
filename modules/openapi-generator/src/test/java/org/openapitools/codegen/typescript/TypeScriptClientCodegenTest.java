package org.openapitools.codegen.typescript;

import com.google.common.collect.Sets;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.*;
import org.openapitools.codegen.CodegenModel;
import org.openapitools.codegen.CodegenOperation;
import org.openapitools.codegen.DefaultCodegen;
import org.openapitools.codegen.TestUtils;
import org.openapitools.codegen.languages.TypeScriptClientCodegen;
import org.openapitools.codegen.utils.ModelUtils;
import org.testng.Assert;
import org.testng.annotations.Test;


public class TypeScriptClientCodegenTest {
    @Test
    public void getTypeDeclarationTest() {
        Schema<?> childSchema = new ArraySchema().items(new StringSchema());

        OpenAPI api = TestUtils.createOpenAPI();
        api.getComponents().addSchemas("Child", childSchema);

        TypeScriptClientCodegen codegen = new TypeScriptClientCodegen();
        codegen.setOpenAPI(api);

        // Cf. issue #4968: Array of Alias of Array
        Schema<?> parentSchema = new ArraySchema().items(
            new Schema().$ref("#/components/schemas/Child")
        );

        ModelUtils.setGenerateAliasAsModel(false);
        Assert.assertEquals(codegen.getTypeDeclaration(parentSchema), "Array<Array<string>>");

        ModelUtils.setGenerateAliasAsModel(true);
        Assert.assertEquals(codegen.getTypeDeclaration(parentSchema), "Array<Child>");

        // Same for Map
        parentSchema = new MapSchema().additionalProperties(new Schema().$ref("#/components/schemas/Child"));

        ModelUtils.setGenerateAliasAsModel(false);
        Assert.assertEquals(codegen.getTypeDeclaration(parentSchema), "{ [key: string]: Array<string>; }");

        ModelUtils.setGenerateAliasAsModel(true);
        Assert.assertEquals(codegen.getTypeDeclaration(parentSchema), "{ [key: string]: Child; }");
    }

    @Test
    public void testComposedSchemasImportTypesIndividually() {
        final TypeScriptClientCodegen codegen = new TypeScriptClientCodegen();
        final OpenAPI openApi = TestUtils.parseFlattenSpec("src/test/resources/3_0/composed-schemas.yaml");
        codegen.setOpenAPI(openApi);
        PathItem path = openApi.getPaths().get("/pets");
        CodegenOperation operation = codegen.fromOperation("/pets", "patch", path.getPatch(), path.getServers());
        // TODO revise the commented test below as oneOf is no longer defined inline
        //but instead defined using $ref with the new inline model resolver in 6.x
        //Assert.assertEquals(operation.imports, Sets.newHashSet("Cat", "Dog"));
        Assert.assertEquals(operation.imports, Sets.newHashSet("PetsPatchRequest"));

    }

    @Test
    public void testArrayWithUniqueItems() {
        final Schema uniqueArray = new ArraySchema()
            .items(new StringSchema())
            .uniqueItems(true);
        final Schema model = new ObjectSchema()
            .description("an object has an array with uniqueItems")
            .addProperties("uniqueArray", uniqueArray)
            .addRequiredItem("uniqueArray");

        final DefaultCodegen codegen = new TypeScriptClientCodegen();
        final OpenAPI openAPI = TestUtils.createOpenAPIWithOneSchema("sample", model);
        codegen.setOpenAPI(openAPI);

        final CodegenModel codegenModel = codegen.fromModel("sample", model);

        Assert.assertFalse(codegenModel.imports.contains("Set"));
    }

    @Test
    public void testWithAdditionalProperties() {
        final Schema inner = new ObjectSchema();
        inner.setAdditionalProperties(true);

        final Schema root = new ObjectSchema()
            .addProperties("inner", inner);

        final DefaultCodegen codegen = new TypeScriptClientCodegen();
        final OpenAPI openAPI = TestUtils.createOpenAPIWithOneSchema("sample", root);
        codegen.setOpenAPI(openAPI);

        try {
            // TypeScriptClientCodegen can generate codes without throwing exception.
            codegen.fromModel("sample", root);
        } catch (Exception e) {
            Assert.fail("Exception was thrown.");
        }
    }

    @Test
    public void defaultModelImportTest() {
        final DefaultCodegen codegen = new TypeScriptClientCodegen();
        Assert.assertEquals(codegen.toModelImport("ApiResponse"), "../models/ApiResponse");
    }

    @Test
    public void modelImportWithMappingTest() {
        final DefaultCodegen codegen = new TypeScriptClientCodegen();
        final String mappedName = "@namespace/dir/response";
        codegen.importMapping().put("ApiResponse", mappedName);
        Assert.assertEquals(codegen.toModelImport("ApiResponse"), mappedName);
    }
}
