/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.model.loader;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.BigDecimalShape;
import software.amazon.smithy.model.shapes.BigIntegerShape;
import software.amazon.smithy.model.shapes.BlobShape;
import software.amazon.smithy.model.shapes.BooleanShape;
import software.amazon.smithy.model.shapes.ByteShape;
import software.amazon.smithy.model.shapes.DocumentShape;
import software.amazon.smithy.model.shapes.DoubleShape;
import software.amazon.smithy.model.shapes.FloatShape;
import software.amazon.smithy.model.shapes.IntegerShape;
import software.amazon.smithy.model.shapes.LongShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShortShape;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.TimestampShape;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.model.traits.AuthDefinitionTrait;
import software.amazon.smithy.model.traits.AuthTrait;
import software.amazon.smithy.model.traits.BoxTrait;
import software.amazon.smithy.model.traits.CorsTrait;
import software.amazon.smithy.model.traits.DeprecatedTrait;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.EndpointTrait;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.traits.ErrorTrait;
import software.amazon.smithy.model.traits.EventHeaderTrait;
import software.amazon.smithy.model.traits.EventPayloadTrait;
import software.amazon.smithy.model.traits.ExamplesTrait;
import software.amazon.smithy.model.traits.ExternalDocumentationTrait;
import software.amazon.smithy.model.traits.HostLabelTrait;
import software.amazon.smithy.model.traits.HttpApiKeyAuthTrait;
import software.amazon.smithy.model.traits.HttpBasicAuthTrait;
import software.amazon.smithy.model.traits.HttpBearerAuthTrait;
import software.amazon.smithy.model.traits.HttpChecksumRequiredTrait;
import software.amazon.smithy.model.traits.HttpDigestAuthTrait;
import software.amazon.smithy.model.traits.HttpErrorTrait;
import software.amazon.smithy.model.traits.HttpHeaderTrait;
import software.amazon.smithy.model.traits.HttpLabelTrait;
import software.amazon.smithy.model.traits.HttpPayloadTrait;
import software.amazon.smithy.model.traits.HttpPrefixHeadersTrait;
import software.amazon.smithy.model.traits.HttpQueryTrait;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.model.traits.IdRefTrait;
import software.amazon.smithy.model.traits.IdempotencyTokenTrait;
import software.amazon.smithy.model.traits.IdempotentTrait;
import software.amazon.smithy.model.traits.JsonNameTrait;
import software.amazon.smithy.model.traits.LengthTrait;
import software.amazon.smithy.model.traits.MediaTypeTrait;
import software.amazon.smithy.model.traits.NoReplaceTrait;
import software.amazon.smithy.model.traits.OptionalAuthTrait;
import software.amazon.smithy.model.traits.PaginatedTrait;
import software.amazon.smithy.model.traits.PatternTrait;
import software.amazon.smithy.model.traits.PrivateTrait;
import software.amazon.smithy.model.traits.ProtocolDefinitionTrait;
import software.amazon.smithy.model.traits.RangeTrait;
import software.amazon.smithy.model.traits.ReadonlyTrait;
import software.amazon.smithy.model.traits.ReferencesTrait;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.traits.RequiresLengthTrait;
import software.amazon.smithy.model.traits.ResourceIdentifierTrait;
import software.amazon.smithy.model.traits.RetryableTrait;
import software.amazon.smithy.model.traits.SensitiveTrait;
import software.amazon.smithy.model.traits.SinceTrait;
import software.amazon.smithy.model.traits.StreamingTrait;
import software.amazon.smithy.model.traits.SuppressTrait;
import software.amazon.smithy.model.traits.TagsTrait;
import software.amazon.smithy.model.traits.TimestampFormatTrait;
import software.amazon.smithy.model.traits.TitleTrait;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.model.traits.TraitFactory;
import software.amazon.smithy.model.traits.UniqueItemsTrait;
import software.amazon.smithy.model.traits.UnstableTrait;
import software.amazon.smithy.model.traits.XmlAttributeTrait;
import software.amazon.smithy.model.traits.XmlFlattenedTrait;
import software.amazon.smithy.model.traits.XmlNameTrait;
import software.amazon.smithy.model.traits.XmlNamespaceTrait;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SetUtils;

/**
 * Represents the prelude model available to every Smithy model.
 *
 * <p>The prelude consists of public prelude shapes like
 * {@code smithy.api#String} and traits that are available to
 * all models like {@code box} and {@code required}.
 *
 * <p>A key design requirement of the prelude is that it cannot require a
 * loaded prelude model in order to reason about the prelude as this would
 * result in infinite recursion while loading the prelude model.
 */
public final class Prelude {
    /** The Smithy prelude namespace. */
    public static final String NAMESPACE = "smithy.api";

    private static final List<Shape> PUBLIC_PRELUDE_SHAPES = ListUtils.of(
            StringShape.builder().id(NAMESPACE + "#String").build(),
            BlobShape.builder().id(NAMESPACE + "#Blob").build(),
            BigIntegerShape.builder().id(NAMESPACE + "#BigInteger").build(),
            BigDecimalShape.builder().id(NAMESPACE + "#BigDecimal").build(),
            TimestampShape.builder().id(NAMESPACE + "#Timestamp").build(),
            DocumentShape.builder().id(NAMESPACE + "#Document").build(),
            BooleanShape.builder().id(NAMESPACE + "#Boolean").addTrait(new BoxTrait()).build(),
            BooleanShape.builder().id(NAMESPACE + "#PrimitiveBoolean").build(),
            ByteShape.builder().id(NAMESPACE + "#Byte").addTrait(new BoxTrait()).build(),
            ByteShape.builder().id(NAMESPACE + "#PrimitiveByte").build(),
            ShortShape.builder().id(NAMESPACE + "#Short").addTrait(new BoxTrait()).build(),
            ShortShape.builder().id(NAMESPACE + "#PrimitiveShort").build(),
            IntegerShape.builder().id(NAMESPACE + "#Integer").addTrait(new BoxTrait()).build(),
            IntegerShape.builder().id(NAMESPACE + "#PrimitiveInteger").build(),
            LongShape.builder().id(NAMESPACE + "#Long").addTrait(new BoxTrait()).build(),
            LongShape.builder().id(NAMESPACE + "#PrimitiveLong").build(),
            FloatShape.builder().id(NAMESPACE + "#Float").addTrait(new BoxTrait()).build(),
            FloatShape.builder().id(NAMESPACE + "#PrimitiveFloat").build(),
            DoubleShape.builder().id(NAMESPACE + "#Double").addTrait(new BoxTrait()).build(),
            DoubleShape.builder().id(NAMESPACE + "#PrimitiveDouble").build());

    /**
     * This list of public prelude traits is manually maintained and must match the
     * public prelude traits defined in prelude-traits.smithy. The Prelude itself
     * cannot refer to the loaded model because the Prelude abstraction is queried
     * while loading the prelude model.
     *
     * <p>A check is made each time the prelude is first loaded to ensure that the
     * actual traits defined in the prelude match the traits in this list. As long
     * as changes are tested before releasing, they should never get out of sync.
     */
    private static final Set<ShapeId> PRELUDE_TRAITS = SetUtils.of(
            AuthTrait.ID,
            BoxTrait.ID,
            CorsTrait.ID,
            DeprecatedTrait.ID,
            DocumentationTrait.ID,
            EndpointTrait.ID,
            EnumTrait.ID,
            ErrorTrait.ID,
            EventHeaderTrait.ID,
            EventPayloadTrait.ID,
            ExamplesTrait.ID,
            ExternalDocumentationTrait.ID,
            HostLabelTrait.ID,
            HttpChecksumRequiredTrait.ID,
            HttpErrorTrait.ID,
            HttpHeaderTrait.ID,
            HttpLabelTrait.ID,
            HttpPayloadTrait.ID,
            HttpPrefixHeadersTrait.ID,
            HttpQueryTrait.ID,
            HttpTrait.ID,
            IdRefTrait.ID,
            IdempotencyTokenTrait.ID,
            IdempotentTrait.ID,
            JsonNameTrait.ID,
            LengthTrait.ID,
            NoReplaceTrait.ID,
            MediaTypeTrait.ID,
            PaginatedTrait.ID,
            PatternTrait.ID,
            PrivateTrait.ID,
            ProtocolDefinitionTrait.ID,
            AuthDefinitionTrait.ID,
            HttpApiKeyAuthTrait.ID,
            HttpBasicAuthTrait.ID,
            HttpDigestAuthTrait.ID,
            HttpBearerAuthTrait.ID,
            OptionalAuthTrait.ID,
            RangeTrait.ID,
            ReadonlyTrait.ID,
            ReferencesTrait.ID,
            RequiresLengthTrait.ID,
            RequiredTrait.ID,
            ResourceIdentifierTrait.ID,
            RetryableTrait.ID,
            SensitiveTrait.ID,
            SinceTrait.ID,
            StreamingTrait.ID,
            SuppressTrait.ID,
            TagsTrait.ID,
            TimestampFormatTrait.ID,
            TitleTrait.ID,
            TraitDefinition.ID,
            UniqueItemsTrait.ID,
            UnstableTrait.ID,
            XmlAttributeTrait.ID,
            XmlFlattenedTrait.ID,
            XmlNameTrait.ID,
            XmlNamespaceTrait.ID);

    private static final Set<ShapeId> PUBLIC_PRELUDE_SHAPE_IDS;

    static {
        PUBLIC_PRELUDE_SHAPE_IDS = PUBLIC_PRELUDE_SHAPES.stream()
                .map(Shape::getId)
                .collect(SetUtils.toUnmodifiableSet());
    }

    private Prelude() {}

    /**
     * Checks if the given shape ID is defined by the prelude.
     *
     * <p>Specifically, this checks if the namespace of the provided shape ID
     * is equal to {@code smithy.api}.
     *
     * @param id Shape ID to check.
     * @return Returns true if the shape is a prelude shape.
     */
    public static boolean isPreludeShape(ToShapeId id) {
        return id.toShapeId().getNamespace().equals(NAMESPACE);
    }

    /**
     * Checks if the given shape is a public shape or trait defined by the
     * prelude.
     *
     * @param id Shape to check.
     * @return Returns true if the shape is a public prelude shape.
     */
    public static boolean isPublicPreludeShape(ToShapeId id) {
        ShapeId toId = id.toShapeId();
        return PUBLIC_PRELUDE_SHAPE_IDS.contains(toId) || PRELUDE_TRAITS.contains(toId);
    }

    // Used by the ModelAssembler to load the prelude into another visitor.
    static Model getPreludeModel() {
        return PreludeHolder.PRELUDE;
    }

    // Lazy initialization holder class idiom for loading prelude traits and shapes.
    private static final class PreludeHolder {
        private static final Model PRELUDE = loadPrelude();

        private static Model loadPrelude() {
            TraitFactory traitFactory = ModelAssembler.LazyTraitFactoryHolder.INSTANCE;
            ModelAssembler assembler = Model.assembler()
                    .disablePrelude()
                    .traitFactory(traitFactory)
                    .addImport(Prelude.class.getResource("prelude-traits.smithy"));

            for (Shape shape : PUBLIC_PRELUDE_SHAPES) {
                assembler.addShape(shape);
            }

            Model preludeModel = assembler.assemble().unwrap();

            // Sanity check to ensure that the prelude model and the tracked prelude traits are consistent.
            // TODO: Can this be moved to a build step in Gradle?
            for (Shape trait : preludeModel.getShapesWithTrait(TraitDefinition.class)) {
                if (!PRELUDE_TRAITS.contains(trait.getId())) {
                    throw new IllegalStateException(
                            "PRELUDE_TRAITS property of prelude is inconsistent with the traits defined in the "
                            + "prelude-traits.smithy file. This property MUST be kept consistent with the file. "
                            + PRELUDE_TRAITS + " in PRELUDE_TRAITS vs "
                            + preludeModel.getShapesWithTrait(TraitDefinition.class).stream()
                                    .map(Shape::getId)
                                    .collect(Collectors.toList()));
                }
            }

            return preludeModel;
        }
    }
}
