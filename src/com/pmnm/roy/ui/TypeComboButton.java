package com.pmnm.roy.ui;

import java.awt.AlphaComposite;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import com.pmnm.risk.globals.localization.Translator;
import com.pmnm.risk.main.Main;
import com.pmnm.risk.toolkit.Utils;

import doa.engine.graphics.DoaGraphicsContext;
import doa.engine.graphics.DoaSprites;
import doa.engine.input.DoaMouse;
import doa.engine.maths.DoaVector;
import doa.engine.ui.button.DoaUIImageButton;

public class TypeComboButton extends DoaUIImageButton {

	private static final long serialVersionUID = -566390496060314364L;

	private static DoaVector bounds;

	public static String[] OPTIONS = new String[] { "CLOSED", "HUMAN", "COMPUTER" };

	public static final List<TypeComboButton> COMBO_BUTTONS = new ArrayList<>();

	public int index = 0;
	boolean isSinglePlayer;

	public TypeComboButton(DoaVector position, boolean isSinglePlayer) {
		super(position, (int) (Main.WINDOW_WIDTH * 0.019f), (int) (Main.WINDOW_HEIGHT * 0.035f), DoaSprites.get("ArrowDownIdle"), DoaSprites.get("ArrowDownIdle"),
		        DoaSprites.get("ArrowDownClick"));
		bounds = new DoaVector(Main.WINDOW_WIDTH * 0.10f, dimensions.y);
		if (isSinglePlayer) {
			OPTIONS = new String[] { "CLOSED", "HUMAN", "COMPUTER" };
		} else {
			OPTIONS = new String[] { "OPEN", "HUMAN", "COMPUTER" };
		}
		this.isSinglePlayer = isSinglePlayer;
		COMBO_BUTTONS.add(this);
	}

	@Override
	public void tick() {
		if (isSinglePlayer || COMBO_BUTTONS.get(COMBO_BUTTONS.size() - 1) != this) {
			if (isEnabled && DoaMouse.MB1) {
				if (click) {
					if (closedHitBox().contains(DoaMouse.X, DoaMouse.Y)) {
						index = 0;
					} else if (humanHitBox().contains(DoaMouse.X, DoaMouse.Y)) {
						index = 1;
					} else if (computerHitBox().contains(DoaMouse.X, DoaMouse.Y) && isSinglePlayer) {
						index = 2;
					}
					click = false;
				} else if (getBounds().contains(DoaMouse.X, DoaMouse.Y) && COMBO_BUTTONS.stream().allMatch(cb -> (!cb.click || (cb.click && cb.noneHit())))) {
					click = !click;
				}
			}
		}
	}

	@Override
	public void render(DoaGraphicsContext g) {
		g.setColor(UIConstants.FONT_COLOR);
		String s = Translator.getInstance().getTranslatedString(OPTIONS[index]);
		g.setFont(UIConstants.UI_FONT.deriveFont(Utils.findMaxFontSizeToFitInArea(g, UIConstants.UI_FONT.deriveFont(1), bounds, s)));
		g.drawImage(DoaSprites.get("PlayerTypeBorder"), position.x - Main.WINDOW_WIDTH * 0.103f, position.y - Main.WINDOW_HEIGHT * 0.003f);
		g.drawString(s.substring(0, 1).toUpperCase() + s.substring(1), position.x - Main.WINDOW_WIDTH * 0.098f, position.y + Main.WINDOW_HEIGHT * 0.029f);
		if (isSinglePlayer || COMBO_BUTTONS.get(COMBO_BUTTONS.size() - 1) != this) {
			super.render(g);
		}
		if (click) {
			int height = DoaSprites.get("DropDownType").getHeight();
			g.drawImage(DoaSprites.get("DropDownType"), position.x - Main.WINDOW_WIDTH * 0.103f, position.y + Main.WINDOW_HEIGHT * 0.040f, Main.WINDOW_WIDTH * 0.124f, height);
			g.pushComposite();
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
			g.drawImage(DoaSprites.get("DropDownTypeTex"), position.x - Main.WINDOW_WIDTH * 0.103f, position.y + Main.WINDOW_HEIGHT * 0.040f, Main.WINDOW_WIDTH * 0.124f, height);
			g.popComposite();
			for (int i = 0; i < OPTIONS.length - (isSinglePlayer ? 0 : 1); i++) {
				s = Translator.getInstance().getTranslatedString(OPTIONS[i]);
				g.drawString(s.substring(0, 1).toUpperCase() + s.substring(1), position.x - Main.WINDOW_WIDTH * 0.098f,
				        position.y + Main.WINDOW_HEIGHT * 0.070f + (Main.WINDOW_HEIGHT * 0.028f * i));
			}
		}
	}

	private Rectangle2D closedHitBox() {
		return new Rectangle2D.Float(position.x - Main.WINDOW_WIDTH * 0.098f, position.y + Main.WINDOW_HEIGHT * 0.046f, Main.WINDOW_WIDTH * 0.115f, dimensions.y * 0.8f);
	}

	private Rectangle2D humanHitBox() {
		return new Rectangle2D.Float(position.x - Main.WINDOW_WIDTH * 0.098f, position.y + Main.WINDOW_HEIGHT * 0.074f, Main.WINDOW_WIDTH * 0.115f, dimensions.y * 0.8f);
	}

	private Rectangle2D computerHitBox() {
		return new Rectangle2D.Float(position.x - Main.WINDOW_WIDTH * 0.098f, position.y + Main.WINDOW_HEIGHT * 0.102f, Main.WINDOW_WIDTH * 0.115f, dimensions.y * 0.8f);
	}

	private boolean noneHit() {
		return !closedHitBox().contains(DoaMouse.X, DoaMouse.Y) && !humanHitBox().contains(DoaMouse.X, DoaMouse.Y) && !computerHitBox().contains(DoaMouse.X, DoaMouse.Y);
	}
}