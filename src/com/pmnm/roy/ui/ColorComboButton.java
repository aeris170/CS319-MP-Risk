package com.pmnm.roy.ui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import com.pmnm.risk.globals.PlayerColorBank;
import com.pmnm.risk.main.Main;
import com.pmnm.risk.toolkit.Utils;

import doa.engine.graphics.DoaGraphicsContext;
import doa.engine.graphics.DoaSprites;
import doa.engine.input.DoaMouse;
import doa.engine.maths.DoaVector;
import doa.engine.ui.button.DoaUIImageButton;

public class ColorComboButton extends DoaUIImageButton {

	private static final long serialVersionUID = 2677754096284998205L;

	private static final BufferedImage[] OPTIONS = new BufferedImage[PlayerColorBank.colors.length];

	static {
		for (int i = 0; i < PlayerColorBank.colors.length; i++) {
			BufferedImage w = DoaSprites.deepCopyDoaSprite(DoaSprites.get("White"));
			Utils.paintImage(w, PlayerColorBank.colors[i]);
			OPTIONS[i] = w;
		}
	}

	public static final List<ColorComboButton> COMBO_BUTTONS = new ArrayList<>();

	public int index = 0;

	public boolean hidden = false;

	public ColorComboButton(DoaVector position) {
		super(position, (int) (Main.WINDOW_WIDTH * 0.019f), (int) (Main.WINDOW_HEIGHT * 0.035f), DoaSprites.get("ArrowDownIdle"), DoaSprites.get("ArrowDownIdle"),
		        DoaSprites.get("ArrowDownClick"));

		index = COMBO_BUTTONS.size();
		COMBO_BUTTONS.add(this);
	}

	@Override
	public void tick() {
		recalibrateBounds();
		if (isEnabled && DoaMouse.MB1) {
			if (click) {
				if (firstColorHitBox().contains(DoaMouse.X, DoaMouse.Y) && COMBO_BUTTONS.stream().filter(cb -> cb != this && cb.isVisible).allMatch(cb -> cb.index != 0)) {
					swapIndex(index, 0);
				} else if (secondColorHitBox().contains(DoaMouse.X, DoaMouse.Y) && COMBO_BUTTONS.stream().filter(cb -> cb != this && cb.isVisible).allMatch(cb -> cb.index != 1)) {
					swapIndex(index, 1);
				} else if (thirdColorHitBox().contains(DoaMouse.X, DoaMouse.Y) && COMBO_BUTTONS.stream().filter(cb -> cb != this && cb.isVisible).allMatch(cb -> cb.index != 2)) {
					swapIndex(index, 2);
				} else if (fourthColorHitBox().contains(DoaMouse.X, DoaMouse.Y) && COMBO_BUTTONS.stream().filter(cb -> cb != this && cb.isVisible).allMatch(cb -> cb.index != 3)) {
					swapIndex(index, 3);
				} else if (fifthColorHitBox().contains(DoaMouse.X, DoaMouse.Y) && COMBO_BUTTONS.stream().filter(cb -> cb != this && cb.isVisible).allMatch(cb -> cb.index != 4)) {
					swapIndex(index, 4);
				} else if (sixthColorHitBox().contains(DoaMouse.X, DoaMouse.Y) && COMBO_BUTTONS.stream().filter(cb -> cb != this && cb.isVisible).allMatch(cb -> cb.index != 5)) {
					swapIndex(index, 5);
				}
				click = false;
			} else if (getBounds().contains(DoaMouse.X, DoaMouse.Y) && COMBO_BUTTONS.stream().allMatch(cb -> (!cb.click || (cb.click && cb.noneHit())))) {
				click = !click;
			}
		}
	}

	@Override
	public void render(DoaGraphicsContext g) {
		if (!hidden) {
			g.setColor(UIConstants.FONT_COLOR);
			g.drawImage(OPTIONS[index], position.x - Main.WINDOW_WIDTH * 0.027f, position.y);

			g.drawImage(DoaSprites.get("ColorBorder"), position.x - Main.WINDOW_WIDTH * 0.029f, position.y - Main.WINDOW_HEIGHT * 0.003f);
			super.render(g);
			if (click) {
				int height = DoaSprites.get("DropDownColor").getHeight();
				g.drawImage(DoaSprites.get("DropDownColor"), position.x - Main.WINDOW_WIDTH * 0.029f, position.y + Main.WINDOW_HEIGHT * 0.040f, Main.WINDOW_WIDTH * 0.050f, height);
				g.pushComposite();
				g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
				g.drawImage(DoaSprites.get("DropDownColorTex"), position.x - Main.WINDOW_WIDTH * 0.029f, position.y + Main.WINDOW_HEIGHT * 0.040f, Main.WINDOW_WIDTH * 0.050f,
				        height);
				g.popComposite();
				for (int i = 0; i < OPTIONS.length; i++) {
					g.drawImage(OPTIONS[i], position.x - Main.WINDOW_WIDTH * 0.027f, position.y + Main.WINDOW_HEIGHT * 0.045f + (Main.WINDOW_HEIGHT * 0.040f * i),
					        OPTIONS[i].getWidth() + Main.WINDOW_WIDTH * 0.020f, OPTIONS[i].getHeight());
				}
				g.draw(firstColorHitBox());
				g.draw(secondColorHitBox());
				g.draw(thirdColorHitBox());
				g.draw(fourthColorHitBox());
				g.draw(fifthColorHitBox());
				g.draw(sixthColorHitBox());
			}
		}
	}

	public Color getColor() {
		return PlayerColorBank.colors[index];
	}

	@Override
	public void hide() {
		super.hide();
		hidden = true;
	}

	@Override
	public void show() {
		super.show();
		if (!isVisible) {
			index = COMBO_BUTTONS.stream().filter(cb -> !(cb.isVisible)).findFirst().orElse(this).index;
		}
		hidden = false;
	}

	private Rectangle2D firstColorHitBox() {
		return new Rectangle2D.Float(position.x - Main.WINDOW_WIDTH * 0.027f, position.y + Main.WINDOW_HEIGHT * 0.045f + (Main.WINDOW_HEIGHT * 0.040f * 0),
		        OPTIONS[0].getWidth() + Main.WINDOW_WIDTH * 0.020f, OPTIONS[0].getHeight());
	}

	private Rectangle2D secondColorHitBox() {
		return new Rectangle2D.Float(position.x - Main.WINDOW_WIDTH * 0.027f, position.y + Main.WINDOW_HEIGHT * 0.045f + (Main.WINDOW_HEIGHT * 0.040f * 1),
		        OPTIONS[1].getWidth() + Main.WINDOW_WIDTH * 0.020f, OPTIONS[1].getHeight());
	}

	private Rectangle2D thirdColorHitBox() {
		return new Rectangle2D.Float(position.x - Main.WINDOW_WIDTH * 0.027f, position.y + Main.WINDOW_HEIGHT * 0.045f + (Main.WINDOW_HEIGHT * 0.040f * 2),
		        OPTIONS[2].getWidth() + Main.WINDOW_WIDTH * 0.020f, OPTIONS[2].getHeight());
	}

	private Rectangle2D fourthColorHitBox() {
		return new Rectangle2D.Float(position.x - Main.WINDOW_WIDTH * 0.027f, position.y + Main.WINDOW_HEIGHT * 0.045f + (Main.WINDOW_HEIGHT * 0.040f * 3),
		        OPTIONS[3].getWidth() + Main.WINDOW_WIDTH * 0.020f, OPTIONS[3].getHeight());
	}

	private Rectangle2D fifthColorHitBox() {
		return new Rectangle2D.Float(position.x - Main.WINDOW_WIDTH * 0.027f, position.y + Main.WINDOW_HEIGHT * 0.045f + (Main.WINDOW_HEIGHT * 0.040f * 4),
		        OPTIONS[4].getWidth() + Main.WINDOW_WIDTH * 0.020f, OPTIONS[4].getHeight());
	}

	private Rectangle2D sixthColorHitBox() {
		return new Rectangle2D.Float(position.x - Main.WINDOW_WIDTH * 0.027f, position.y + Main.WINDOW_HEIGHT * 0.045f + (Main.WINDOW_HEIGHT * 0.040f * 5),
		        OPTIONS[5].getWidth() + Main.WINDOW_WIDTH * 0.020f, OPTIONS[5].getHeight());
	}

	private boolean noneHit() {
		return !firstColorHitBox().contains(DoaMouse.X, DoaMouse.Y) && !secondColorHitBox().contains(DoaMouse.X, DoaMouse.Y) && !thirdColorHitBox().contains(DoaMouse.X, DoaMouse.Y)
		        && !fourthColorHitBox().contains(DoaMouse.X, DoaMouse.Y) && !fifthColorHitBox().contains(DoaMouse.X, DoaMouse.Y)
		        && !sixthColorHitBox().contains(DoaMouse.X, DoaMouse.Y);
	}

	private static void swapIndex(int i1, int i2) {
		BufferedImage tmp = OPTIONS[i1];
		OPTIONS[i1] = OPTIONS[i2];
		OPTIONS[i2] = tmp;

		Color c = PlayerColorBank.colors[i1];
		PlayerColorBank.colors[i1] = PlayerColorBank.colors[i2];
		PlayerColorBank.colors[i2] = c;
	}
}
