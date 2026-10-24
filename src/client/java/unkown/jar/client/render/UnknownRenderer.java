package unkown.jar.client.render;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.util.Mth;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.Identifier;
import unkown.jar.UnkownJar;
import unkown.jar.entity.UnknownEntity;

public class UnknownRenderer extends HumanoidMobRenderer<UnknownEntity, UnknownRenderState, UnknownModel> {
	private static final Identifier[] TEXTURES = {
			UnkownJar.id("textures/entity/phase1.png"),
			UnkownJar.id("textures/entity/phase1.png"),
			UnkownJar.id("textures/entity/phase2.png"),
			UnkownJar.id("textures/entity/phase3.png"),
			UnkownJar.id("textures/entity/phase4.png")
	};

	public UnknownRenderer(EntityRendererProvider.Context context) {
		super(context, new UnknownModel(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
	}

	@Override
	public UnknownRenderState createRenderState() {
		return new UnknownRenderState();
	}

	@Override
	public void extractRenderState(UnknownEntity entity, UnknownRenderState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.phase = Math.max(1, Math.min(4, entity.getPhase()));
		state.hunting = entity.isHunting();
	}

	@Override
	public Identifier getTextureLocation(UnknownRenderState state) {
		return TEXTURES[Math.max(1, Math.min(4, state.phase))];
	}
}

class UnknownRenderState extends HumanoidRenderState {
	public int phase = 1;
	public boolean hunting;
}

/**
 * Humanoides Modell mit eigenen, phasenabhaengigen Posen:
 * P1 regungslos mit leichtem Schwanken, P2 schiefer Kopf und zuckende Haende,
 * P3 vorgebeugt mit ausgestreckten Armen, P4 hochgerissene, zitternde Arme und ruckartig verdrehter Kopf.
 */
class UnknownModel extends HumanoidModel<UnknownRenderState> {
	public UnknownModel(ModelPart root) {
		super(root);
	}

	@Override
	public void setupAnim(UnknownRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks;

		switch (state.phase) {
			case 1 -> {
				this.head.zRot = 0.10F * Mth.sin(t * 0.05F);
				this.rightArm.xRot *= 0.2F;
				this.leftArm.xRot *= 0.2F;
				this.rightArm.zRot += 0.04F * Mth.sin(t * 0.09F) + 0.04F;
				this.leftArm.zRot -= 0.04F * Mth.sin(t * 0.09F + 1.0F) + 0.04F;
			}
			case 2 -> {
				this.head.zRot = 0.45F + 0.05F * Mth.sin(t * 0.8F);
				this.head.xRot += 0.25F;
				this.rightArm.xRot = -0.25F + 0.2F * Mth.sin(t * 0.45F);
				this.leftArm.xRot = -0.15F + 0.2F * Mth.sin(t * 0.45F + 2.0F);
				this.rightArm.zRot = 0.12F;
				this.leftArm.zRot = -0.12F;
				this.body.xRot = 0.15F;
			}
			case 3 -> {
				// Gebrochener Hals, ein Arm haengt, der andere zeigt auf dich; bei der Jagd weit ausgebreitete Arme.
				this.head.zRot = 0.95F + 0.04F * Mth.sin(t * 0.9F);
				this.head.xRot += 0.2F;
				if (state.hunting) {
					this.rightArm.xRot = -1.2F + 0.1F * Mth.sin(t * 1.6F);
					this.leftArm.xRot = -1.2F + 0.1F * Mth.sin(t * 1.6F + 1.7F);
					this.rightArm.zRot = 0.9F;
					this.leftArm.zRot = -0.9F;
					this.body.xRot = 0.6F;
				} else {
					this.rightArm.xRot = 0.0F;
					this.rightArm.zRot = 0.05F;
					this.leftArm.xRot = -1.45F;
					this.leftArm.zRot = -0.1F + 0.03F * Mth.sin(t * 0.5F);
					this.body.xRot = 0.15F;
				}
			}
			default -> {
				float raise = state.hunting ? -2.7F : -1.2F;
				float shake = state.hunting ? 0.2F : 0.08F;
				this.rightArm.xRot = raise + shake * Mth.sin(t * 2.3F);
				this.leftArm.xRot = raise + shake * Mth.sin(t * 2.3F + 2.1F);
				this.rightArm.zRot = 0.45F + 0.1F * Mth.sin(t * 1.9F);
				this.leftArm.zRot = -0.45F - 0.1F * Mth.sin(t * 1.9F + 1.0F);
				this.body.xRot = state.hunting ? 0.35F : 0.1F;
				// Ruckartiges Verdrehen des Kopfes, alle paar Sekunden.
				int cycle = (int) t % 60;
				if (cycle < 4) {
					this.head.zRot = 1.1F;
					this.head.yRot += 0.9F;
				} else {
					this.head.zRot = 0.2F * Mth.sin(t * 0.6F);
				}
			}
		}
	}
}
