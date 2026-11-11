package com.aetherius.client.render;

import com.aetherius.entity.NoxThalEntity;
import com.aetherius.util.Aeth;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

/**
 * Nox'Thal, the eclipse dragon.
 *
 * <p>Build rules, learned the hard way:</p>
 * <ul>
 *   <li>Every pivot sits on a real joint in <b>absolute model space</b> (feet at y = 24, as
 *       {@code LivingEntityRenderer} expects after its own {@code -1.501} translation).</li>
 *   <li>Cuboid offsets are relative to that pivot only — never mixed with body-space numbers.</li>
 *   <li>Anything that must rotate together (skull + jaw + horns, wing bones + membrane) lives in
 *       <b>one</b> part with several cuboids, so animation cannot tear the silhouette apart.</li>
 *   <li>Silhouette comes from many small tapered boxes: chest + hips + spine plates, an S-curved
 *       two-segment neck, a wedged skull with jaw and swept horns, a four-bone wing with three
 *       membranes and a claw, a three-segment tail with two fin blades, and four two-bone legs.</li>
 * </ul>
 */
public class NoxThalModel extends EntityModel<NoxThalEntity> {

    public static final EntityModelLayer LAYER = new EntityModelLayer(Aeth.id("nox_thal"), "main");

    /** Shared UV rectangles: one patch per material, so no box can sample a wrong region. */
    private static final int UV_SCALE_X = 0;
    private static final int UV_SCALE_Y = 0;
    private static final int UV_MEMBRANE_X = 0;
    private static final int UV_MEMBRANE_Y = 96;
    private static final int UV_BONE_X = 100;
    private static final int UV_BONE_Y = 0;

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart leftWing;
    private final ModelPart rightWing;
    private final ModelPart tail;
    private final ModelPart frontLeftLeg;
    private final ModelPart frontRightLeg;
    private final ModelPart backLeftLeg;
    private final ModelPart backRightLeg;

    public NoxThalModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.neck = root.getChild("neck");
        this.head = neck.getChild("head");
        this.leftWing = root.getChild("left_wing");
        this.rightWing = root.getChild("right_wing");
        this.tail = root.getChild("tail");
        this.frontLeftLeg = root.getChild("front_left_leg");
        this.frontRightLeg = root.getChild("front_right_leg");
        this.backLeftLeg = root.getChild("back_left_leg");
        this.backRightLeg = root.getChild("back_right_leg");
    }

    private static ModelPartBuilder scale() {
        return ModelPartBuilder.create().uv(UV_SCALE_X, UV_SCALE_Y);
    }

    private static ModelPartBuilder membrane() {
        return ModelPartBuilder.create().uv(UV_MEMBRANE_X, UV_MEMBRANE_Y);
    }

    private static ModelPartBuilder bone() {
        return ModelPartBuilder.create().uv(UV_BONE_X, UV_BONE_Y);
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();

        // ---------------------------------------------------------------- torso
        // chest (deep) + hips (tapered) + shoulder hump + three spine plates
        root.addChild("body",
                scale()
                        .cuboid(-10.0f, -8.0f, -12.0f, 20.0f, 16.0f, 22.0f)
                        .cuboid(-7.0f, -6.0f, 10.0f, 14.0f, 12.0f, 12.0f)
                        .cuboid(-8.0f, -10.0f, -10.0f, 16.0f, 3.0f, 10.0f)
                        .cuboid(-1.0f, -11.0f, -6.0f, 2.0f, 3.0f, 5.0f)
                        .cuboid(-1.0f, -11.0f, 1.0f, 2.0f, 4.0f, 5.0f)
                        .cuboid(-1.0f, -9.0f, 9.0f, 2.0f, 3.0f, 5.0f),
                ModelTransform.pivot(0.0f, 15.0f, 0.0f));

        // ---------------------------------------------------------------- neck
        // Two segments that step *forward* as they rise, so the head leads the body instead of
        // sitting on top of a chimney.
        ModelPartData neck = root.addChild("neck",
                scale()
                        .cuboid(-5.0f, -6.0f, -6.0f, 10.0f, 7.0f, 11.0f)
                        .cuboid(-4.5f, -12.0f, -13.0f, 9.0f, 7.0f, 10.0f),
                ModelTransform.pivot(0.0f, 7.0f, -10.0f));

        // ---------------------------------------------------------------- head
        // Parented to the neck (pivot = head pivot minus neck pivot) so tilting the neck carries
        // the whole head with it. Skull, brow, jaw, snout, crest and horns are one part, so head
        // rotation can never split the face either.
        neck.addChild("head",
                scale()
                        .cuboid(-7.0f, -6.0f, -9.0f, 14.0f, 13.0f, 12.0f)      // skull
                        .cuboid(-8.0f, -8.0f, -6.0f, 16.0f, 3.0f, 9.0f)        // brow ridge
                        .cuboid(-4.0f, 0.0f, -19.0f, 8.0f, 6.0f, 10.0f)        // snout
                        .cuboid(-5.0f, 5.0f, -17.0f, 10.0f, 3.0f, 13.0f)       // jaw
                        .cuboid(-1.0f, -14.0f, -7.0f, 2.0f, 6.0f, 11.0f)       // crest fin
                        .cuboid(-8.0f, -12.0f, -9.0f, 3.0f, 6.0f, 6.0f)        // horn, swept back
                        .cuboid(-10.0f, -16.0f, -5.0f, 3.0f, 5.0f, 6.0f)
                        .mirrored().cuboid(5.0f, -12.0f, -9.0f, 3.0f, 6.0f, 6.0f)
                        .mirrored().cuboid(7.0f, -16.0f, -5.0f, 3.0f, 5.0f, 6.0f),
                ModelTransform.pivot(0.0f, -18.0f, -15.0f));

        // ---------------------------------------------------------------- wings
        // Leading-edge bones (humerus, forearm, finger, claw) plus four membrane panels that
        // share edges, so the wing reads as one swept surface instead of separate plates.
        root.addChild("left_wing",
                scale()
                        .cuboid(0.0f, -1.0f, -4.0f, 12.0f, 3.0f, 4.0f)
                        .cuboid(12.0f, -1.0f, -4.0f, 14.0f, 2.0f, 3.0f)
                        .cuboid(26.0f, -1.0f, -4.0f, 16.0f, 2.0f, 2.0f)
                        .cuboid(42.0f, -1.0f, -4.0f, 5.0f, 2.0f, 2.0f)
                .uv(UV_MEMBRANE_X, UV_MEMBRANE_Y)
                        .cuboid(6.0f, -1.0f, -2.0f, 18.0f, 2.0f, 16.0f)
                        .cuboid(24.0f, -1.0f, -2.0f, 14.0f, 2.0f, 12.0f)
                        .cuboid(38.0f, -1.0f, -2.0f, 9.0f, 2.0f, 8.0f)
                        .cuboid(0.0f, -1.0f, 0.0f, 8.0f, 2.0f, 16.0f),
                ModelTransform.pivot(9.0f, 11.0f, -6.0f));

        root.addChild("right_wing",
                scale()
                        .cuboid(-12.0f, -1.0f, -4.0f, 12.0f, 3.0f, 4.0f)
                        .cuboid(-26.0f, -1.0f, -4.0f, 14.0f, 2.0f, 3.0f)
                        .cuboid(-42.0f, -1.0f, -4.0f, 16.0f, 2.0f, 2.0f)
                        .cuboid(-47.0f, -1.0f, -4.0f, 5.0f, 2.0f, 2.0f)
                .uv(UV_MEMBRANE_X, UV_MEMBRANE_Y)
                        .cuboid(-24.0f, -1.0f, -2.0f, 18.0f, 2.0f, 16.0f)
                        .cuboid(-38.0f, -1.0f, -2.0f, 14.0f, 2.0f, 12.0f)
                        .cuboid(-47.0f, -1.0f, -2.0f, 9.0f, 2.0f, 8.0f)
                        .cuboid(-8.0f, -1.0f, 0.0f, 8.0f, 2.0f, 16.0f),
                ModelTransform.pivot(-9.0f, 11.0f, -6.0f));

        // ---------------------------------------------------------------- tail
        // Three tapering segments plus a *vertical* fin, like a dragon's paddle tail.
        root.addChild("tail",
                scale()
                        .cuboid(-3.0f, -3.0f, 0.0f, 6.0f, 6.0f, 14.0f)
                        .cuboid(-2.0f, -2.0f, 14.0f, 4.0f, 4.0f, 12.0f)
                        .cuboid(-1.0f, -1.0f, 26.0f, 2.0f, 2.0f, 10.0f)
                        .cuboid(-1.0f, -10.0f, 30.0f, 2.0f, 9.0f, 8.0f)
                        .cuboid(-1.0f, -7.0f, 36.0f, 2.0f, 6.0f, 6.0f),
                ModelTransform.pivot(0.0f, 12.0f, 20.0f));

        // ---------------------------------------------------------------- legs
        // Four two-bone legs; feet land exactly on the y = 24 ground plane.
        root.addChild("front_left_leg", leg(), ModelTransform.pivot(7.0f, 7.0f, -6.0f));
        root.addChild("front_right_leg", leg(true), ModelTransform.pivot(-7.0f, 7.0f, -6.0f));
        root.addChild("back_left_leg", leg(), ModelTransform.pivot(6.0f, 7.0f, 12.0f));
        root.addChild("back_right_leg", leg(true), ModelTransform.pivot(-6.0f, 7.0f, 12.0f));

        return TexturedModelData.of(data, 256, 256);
    }

    private static ModelPartBuilder leg() {
        return leg(false);
    }

    private static ModelPartBuilder leg(boolean mirrored) {
        ModelPartBuilder builder = scale();
        if (mirrored) {
            builder = builder.mirrored();
        }
        // scaled thigh + shin, bone foot: 16 px reach from the torso underside lands on y = 24
        return builder.cuboid(-3.5f, 0.0f, -3.5f, 7.0f, 11.0f, 7.0f)
                .cuboid(-2.5f, 11.0f, -2.5f, 5.0f, 4.0f, 5.0f)
                .uv(UV_BONE_X, UV_BONE_Y)
                .cuboid(-3.5f, 15.0f, -4.0f, 7.0f, 2.0f, 8.0f);
    }

    @Override
    public void setAngles(NoxThalEntity entity, float limbAngle, float limbDistance,
                          float animationProgress, float headYaw, float headPitch) {
        int phase = entity.phaseIndex();
        boolean grounded = entity.isGrounded();

        float bob = MathHelper.sin(animationProgress * 0.06f);
        // Root motion only: the torso, neck, wings, tail and legs are separate parts, so animating
        // the torso alone would slide it out of the neck.
        root.pitch = grounded ? 0.06f : 0.02f;
        root.roll = bob * 0.02f;
        root.pivotY = grounded ? 1.2f : bob * 0.6f;

        neck.pitch = (grounded ? 0.3f : 0.12f) + MathHelper.sin(animationProgress * 0.05f) * 0.04f;
        head.yaw = headYaw * 0.017453292f;
        head.pitch = headPitch * 0.017453292f - 0.08f;

        // Wing beats: fast in Phase 1, heavy in Phase 2, spread and almost static in Phase 3.
        float speed = phase == 0 ? 0.35f : phase == 1 ? 0.18f : 0.1f;
        float beat = MathHelper.sin(animationProgress * speed);
        float flap = beat * (phase == 0 ? 0.5f : phase == 1 ? 0.34f : 0.22f);
        // Dihedral: a wing that starts flat reads as a glider, not a dragon.
        leftWing.roll = -0.16f - flap;
        rightWing.roll = 0.16f + flap;
        leftWing.yaw = 0.22f + beat * 0.05f;
        rightWing.yaw = -0.22f - beat * 0.05f;
        leftWing.pitch = 0.04f;
        rightWing.pitch = 0.04f;

        float sway = MathHelper.sin(animationProgress * 0.08f);
        tail.yaw = sway * 0.28f;
        tail.pitch = 0.06f + sway * 0.06f;

        // Walk cycle only matters when it lands; in the air the legs trail behind.
        float swing = MathHelper.cos(limbAngle * 0.6662f) * 0.6f * limbDistance;
        if (grounded) {
            frontLeftLeg.pitch = swing;
            frontRightLeg.pitch = -swing;
            backLeftLeg.pitch = -swing;
            backRightLeg.pitch = swing;
        } else {
            frontLeftLeg.pitch = 0.55f;
            frontRightLeg.pitch = 0.45f;
            backLeftLeg.pitch = -0.35f;
            backRightLeg.pitch = -0.25f;
        }
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay,
                       float red, float green, float blue, float alpha) {
        this.root.render(matrices, vertices, light, overlay, red, green, blue, alpha);
    }
}
