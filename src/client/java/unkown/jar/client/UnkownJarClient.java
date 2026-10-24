package unkown.jar.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import unkown.jar.client.render.UnknownRenderer;
import unkown.jar.entity.ModEntities;

public class UnkownJarClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(ModEntities.UNKNOWN, UnknownRenderer::new);
		DeathCrash.init();
	}
}
