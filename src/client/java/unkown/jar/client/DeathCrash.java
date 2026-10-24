package unkown.jar.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.MinecraftServer;
import unkown.jar.UnkownConfig;
import unkown.jar.UnkownJar;
import unkown.jar.entity.ModSounds;
import unkown.jar.entity.Phases;

import java.util.Random;

/**
 * Wenn ein unknown-Entity den lokalen Spieler toetet, stuerzt das Spiel kurz nach dem Todesbildschirm ab.
 * Erkennung ueber die Todesnachricht ("death.attack.*" mit dem Namen des Entities als Toeter).
 * Es werden keine Dateien veraendert; im Einzelspieler wird die Welt vorher gespeichert.
 * Abschaltbar ueber "crashOnDeath": false in config/unkownjar.json.
 */
public final class DeathCrash {
	private static int countdown = -1;

	public static void init() {
		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			if (overlay || countdown >= 0) {
				return;
			}
			UnkownConfig cfg = UnkownConfig.get();
			int phase = killerPhase(message);
			if (phase == 0 || !(cfg.crashOnDeath || cfg.jumpscare)) {
				return;
			}
			Minecraft mc = Minecraft.getInstance();
			if (cfg.jumpscare) {
				mc.execute(() -> {
					mc.setScreen(new JumpscareScreen(phase));
					// Mehrfach uebereinander = noch lauter; laeuft ueber MASTER, ignoriert also den Monster-Regler.
					mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.JUMPSCARE, 1.0F, 1.0F));
					mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.JUMPSCARE, 0.92F, 1.0F));
					mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.JUMPSCARE, 0.7F, 1.0F));
				});
			}
			if (cfg.crashOnDeath) {
				countdown = cfg.jumpscare ? JumpscareScreen.DURATION_TICKS + 15 : 60;
				saveWorldIfSingleplayer();
			} else {
				countdown = -1;
			}
		});
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (countdown < 0) {
				return;
			}
			if (client.player == null || client.level == null) {
				countdown = -1; // Welt schon verlassen
				return;
			}
			if (--countdown == 0) {
				countdown = -1;
				CrashReport report = CrashReport.forThrowable(
						new IllegalStateException("unknown.jar: It found you. This was not a bug."),
						"unknown.jar took your game");
				throw new ReportedException(report);
			}
		});
	}

	/** @return Phase (1-4) des Toeters, oder 0 wenn der lokale Spieler nicht von einem unknown-Entity getoetet wurde. */
	private static int killerPhase(Component message) {
		if (!(message.getContents() instanceof TranslatableContents tc) || !tc.getKey().startsWith("death.attack.")) {
			return 0;
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return 0;
		}
		String self = mc.player.getName().getString();
		boolean selfIsVictim = false;
		int killerPhase = 0;
		for (Object arg : tc.getArgs()) {
			String text = arg instanceof Component c ? c.getString() : String.valueOf(arg);
			if (text.equals(self)) {
				selfIsVictim = true;
			}
			for (int i = 1; i <= 4; i++) {
				if (text.equals(Phases.name(i))) {
					killerPhase = i;
				}
			}
		}
		return selfIsVictim ? killerPhase : 0;
	}

	private static void saveWorldIfSingleplayer() {
		try {
			MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
			if (server != null) {
				server.execute(() -> server.saveEverything(true, false, true));
			}
		} catch (Exception e) {
			UnkownJar.LOGGER.warn("unknown.jar: saving before the crash failed (ignored)", e);
		}
	}

	private DeathCrash() {
	}
}

/**
 * Vollbild-Jumpscare, komplett aus gefuellten Rechtecken gezeichnet (keine Textur noetig):
 * Blitz, hineinzoomendes Gesicht mit leeren Augen und aufklappendem Maul, Zittern, Rauschen.
 */
class JumpscareScreen extends Screen {
	public static final int DURATION_TICKS = 70;

	private final int phase;
	private int ticks;

	public JumpscareScreen(int phase) {
		super(Component.literal("unknown"));
		this.phase = Math.max(2, Math.min(4, phase));
	}

	@Override
	public void tick() {
		this.ticks++;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		int w = this.width;
		int h = this.height;
		float time = this.ticks + partialTick;
		float progress = Math.min(1.0F, time / DURATION_TICKS);
		Random r = new Random(this.ticks * 7919L + (long) (partialTick * 3));

		// Hintergrund + Blitze in den ersten Ticks
		int bg = 0xFF000000;
		if (this.ticks < 8 && (this.ticks % 2 == 0)) {
			bg = 0xFFFFFFFF;
		} else if (this.ticks < 14 && r.nextInt(4) == 0) {
			bg = 0xFF550000;
		}
		g.fill(0, 0, w, h, bg);

		float u = Math.min(w, h) / 30.0F * (0.9F + progress * 1.1F);
		int cx = w / 2 + (int) ((r.nextFloat() - 0.5F) * u * 1.4F);
		int cy = h / 2 + (int) ((r.nextFloat() - 0.5F) * u * 1.4F);

		int skin = switch (this.phase) {
			case 2 -> 0xFF86868C;
			case 3 -> 0xFF707A72;
			default -> 0xFFE4E2D6;
		};
		int dark = 0xFF000000;
		int tooth = 0xFFEBE8DC;

		// Kopf
		rect(g, cx, cy, -9, -12, 9, 10, u, skin);
		rect(g, cx, cy, -7, 10, 7, 14, u, skin);
		rect(g, cx, cy, -10, -14, 10, -11, u, 0xFF101010);          // Haaransatz/Kapuze

		// Augenhoehlen
		float wobble = (float) Math.sin(time * 0.6F);
		rect(g, cx, cy, -7.5F, -7, -2, 0, u, dark);
		rect(g, cx, cy, 2, -7, 7.5F, 0, u, dark);
		int pupil = this.phase == 3 ? 0xFFFFFFFF : 0xFFCC0000;
		float pw = 0.8F + progress * 0.9F;
		rect(g, cx, cy, -5 + wobble, -4, -5 + wobble + pw, -4 + pw, u, pupil);
		rect(g, cx, cy, 4 + wobble, -4, 4 + wobble + pw, -4 + pw, u, pupil);

		// Nase
		rect(g, cx, cy, -1.2F, 1, -0.3F, 4, u, 0xFF222222);
		rect(g, cx, cy, 0.3F, 1, 1.2F, 4, u, 0xFF222222);

		// Maul klappt auf
		float open = 2.0F + 7.0F * (float) Math.min(1.0, time / 18.0F) + (float) Math.abs(Math.sin(time * 1.3F)) * 1.5F;
		float top = 5.0F;
		rect(g, cx, cy, -8, top, 8, top + open, u, dark);
		for (int i = -8; i < 8; i += 2) {
			rect(g, cx, cy, i, top, i + 1.1F, top + 2.0F, u, tooth);
			rect(g, cx, cy, i + 1, top + open - 2.0F, i + 2.1F, top + open, u, tooth);
		}

		// Blutige Traenen
		int blood = 0xFF8A0000;
		rect(g, cx, cy, -5.5F, 0, -4.8F, 3 + progress * 10, u, blood);
		rect(g, cx, cy, 4.8F, 0, 5.5F, 4 + progress * 8, u, blood);

		// Rauschen
		for (int i = 0; i < 140; i++) {
			int x = r.nextInt(w);
			int y = r.nextInt(h);
			int c = r.nextBoolean() ? 0x88FFFFFF : 0x88000000;
			g.fill(x, y, x + 1 + r.nextInt(4), y + 1 + r.nextInt(2), c);
		}
		// Zeilen-Glitch
		if (r.nextInt(3) == 0) {
			int y = r.nextInt(h);
			g.fill(0, y, w, y + 2 + r.nextInt(6), 0x66FF0000);
		}
		// Rote Vignette
		int edge = (int) (Math.min(w, h) * (0.10F + 0.12F * progress));
		g.fill(0, 0, w, edge, 0x66AA0000);
		g.fill(0, h - edge, w, h, 0x66AA0000);
		g.fill(0, 0, edge, h, 0x66AA0000);
		g.fill(w - edge, 0, w, h, 0x66AA0000);

		// Name am Ende
		if (this.ticks > 40) {
			g.drawCenteredString(this.font, Component.literal(Phases.name(this.phase)), w / 2, h - 28, 0xFFAA0000);
		}
	}

	private static void rect(GuiGraphics g, int cx, int cy, float x0, float y0, float x1, float y1, float u, int color) {
		g.fill(cx + Math.round(x0 * u), cy + Math.round(y0 * u), cx + Math.round(x1 * u), cy + Math.round(y1 * u), color);
	}
}
