package com.pmnm.roy.ui.menu;

import java.awt.image.BufferedImage;

import com.pmnm.roy.ui.UIConstants;

import doa.engine.core.DoaGraphicsFunctions;
import doa.engine.scene.DoaObject;
import doa.engine.scene.elements.renderers.DoaRenderer;
import lombok.Setter;

public class Embroidments extends DoaObject {

	private static final long serialVersionUID = 3007272624368964821L;

	@Setter private boolean isVisible = true;

	private transient BufferedImage fleurDeLis;
	private transient BufferedImage topRing;
	private transient BufferedImage bottomRing;

	public Embroidments() {
		this.fleurDeLis = UIConstants.getFleurDeLis();
		this.topRing = UIConstants.getTopRing();
		this.bottomRing = UIConstants.getBottomRing();

		addComponent(new Renderer());
	}

	private final class Renderer extends DoaRenderer {

		private static final long serialVersionUID = 4309693562331539996L;

		@Override
		public void render() {
			if (!isVisible) { return; }

			for (int x = 0; x < 1920; x += UIConstants.FLEUR_WIDTH - 1) { // on big displays, fleur has gaps between. -1 fixes that
				DoaGraphicsFunctions.drawImage(fleurDeLis, x, 0,									UIConstants.FLEUR_WIDTH, UIConstants.FLEUR_HEIGHT);
				DoaGraphicsFunctions.drawImage(fleurDeLis, x, UIConstants.FLEUR_HEIGHT, 			UIConstants.FLEUR_WIDTH, UIConstants.FLEUR_HEIGHT);
				DoaGraphicsFunctions.drawImage(fleurDeLis, x, 1080 - UIConstants.FLEUR_HEIGHT * 2f,	UIConstants.FLEUR_WIDTH, UIConstants.FLEUR_HEIGHT);
				DoaGraphicsFunctions.drawImage(fleurDeLis, x, 1080 - UIConstants.FLEUR_HEIGHT, 		UIConstants.FLEUR_WIDTH, UIConstants.FLEUR_HEIGHT);
			}

			DoaGraphicsFunctions.drawImage(
				topRing,
				0, UIConstants.FLEUR_HEIGHT * 1.5f,
				1920, bottomRing.getHeight()
			);
			DoaGraphicsFunctions.drawImage(
				bottomRing,
				0, 1080 - UIConstants.FLEUR_HEIGHT * 1.5f - bottomRing.getHeight(),
				1920, bottomRing.getHeight()
			);
		}
	}
}
