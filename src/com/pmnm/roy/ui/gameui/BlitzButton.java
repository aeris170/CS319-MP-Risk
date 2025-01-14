package com.pmnm.roy.ui.gameui;

import java.awt.image.BufferedImage;

import doa.engine.graphics.DoaGraphicsContext;
import doa.engine.ui.button.DoaUIImageButton;

public class BlitzButton extends DoaUIImageButton {

	private static final long serialVersionUID = -4633067220666124307L;

	public static BlitzButton INSTANCE;

	private transient BufferedImage idleImage;
	private transient BufferedImage hoverImage;
	private transient BufferedImage clickImage;

	public BlitzButton(float x, float y, int width, int height, BufferedImage idleImage, BufferedImage hoverImage) {
		super(x, y, width, height, idleImage, hoverImage);
		this.idleImage = idleImage;
		this.hoverImage = hoverImage;
		this.clickImage = hoverImage;
		INSTANCE = this;
	}

	@Override
	public void render(DoaGraphicsContext g) {
		if (click) {
			g.drawImage(clickImage, position.x, position.y - 84, getDimensions().x, getDimensions().y + 99);
		} else if (hover) {
			g.drawImage(hoverImage, position.x, position.y - 84, getDimensions().x, getDimensions().y + 99);
		} else {
			g.drawImage(idleImage, position.x, position.y - 84, getDimensions().x, getDimensions().y + 99);
		}
	}
}