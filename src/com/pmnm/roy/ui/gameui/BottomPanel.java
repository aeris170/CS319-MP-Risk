package com.pmnm.roy.ui.gameui;

import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;

import com.pmnm.risk.globals.Globals;
import com.pmnm.risk.globals.ZOrders;
import com.pmnm.risk.globals.localization.Translator;
import com.pmnm.roy.RoyImageButton;
import com.pmnm.roy.RoyMenu;
import com.pmnm.roy.ui.UIConstants;
import com.pmnm.roy.ui.UIUtils;

import doa.engine.core.DoaGraphicsFunctions;
import doa.engine.graphics.DoaSprites;
import doa.engine.maths.DoaMath;
import doa.engine.maths.DoaVector;
import doa.engine.scene.elements.renderers.DoaRenderer;
import doa.engine.scene.elements.scripts.DoaScript;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import pmnm.risk.game.Deploy;
import pmnm.risk.game.IProvince;
import pmnm.risk.game.IRiskGameContext.TurnPhase;
import pmnm.risk.game.Reinforce;
import pmnm.risk.game.databasedimpl.Province;
import pmnm.risk.game.databasedimpl.RiskGameContext;
import pmnm.risk.map.board.ProvinceHitArea;
import pmnm.risk.map.board.ProvinceHitAreas;

@SuppressWarnings("serial")
public class BottomPanel extends RoyMenu {

	private RoyImageButton nextPhaseButton;
	private RoyImageButton decrementButton;
	private RoyImageButton incrementButton;
	private RoyImageButton centerPieceButton;

	private final DoaVector NEXT_PHASE_POSITION		= new DoaVector(1200, 965);
	private final DoaVector DECREMENT_POSITION		= new DoaVector(700, 1035);
	private final DoaVector INCREMENT_POSITION		= new DoaVector(700, 985);
	private final DoaVector CENTER_PIECE_POSITION	= new DoaVector(615, 1000);

	private int maxTroopCount = Globals.UNKNOWN_TROOP_COUNT;
	private int selectedTroopCount = Globals.UNKNOWN_TROOP_COUNT;

	private final RiskGameContext context;

	private Province clickedProvince = null;

	private String garrisonText = "";
	private String ownerText = "";
	private String nameText = "";
	private String continentText = "";

	private String currentPhaseText = "";

	public BottomPanel(final RiskGameContext context) {
		this.context = context;

		nextPhaseButton = RoyImageButton.builder()
			.image(DoaSprites.getSprite("nextPhaseButtonIdle"))
			.hoverImage(DoaSprites.getSprite("nextPhaseButtonHover"))
			.pressImage(DoaSprites.getSprite("nextPhaseButtonPressed"))
			.disabledImage(DoaSprites.getSprite("nextPhaseButtonDisabled"))
			.action(source -> {
				context.goToNextPhase();
				maxTroopCount = Globals.UNKNOWN_TROOP_COUNT;
				selectedTroopCount = Globals.UNKNOWN_TROOP_COUNT;
				if (context.getCurrentPhase() == TurnPhase.DRAFT) {
					nextPhaseButton.setEnabled(false);
				}
			})
			.build();
		nextPhaseButton.setPosition(NEXT_PHASE_POSITION);
		nextPhaseButton.setEnabled(false);
		nextPhaseButton.setScale(.7f);
		addElement(nextPhaseButton);

		decrementButton = RoyImageButton.builder()
			.image(DoaSprites.getSprite("arrowDown"))
			.hoverImage(DoaSprites.getSprite("arrowDownHover"))
			.pressImage(DoaSprites.getSprite("arrowDownPress"))
			.action(source -> decrementTroopCount())
			.build();
		decrementButton.setPosition(DECREMENT_POSITION);
		addElement(decrementButton);

		incrementButton = RoyImageButton.builder()
			.image(DoaSprites.getSprite("arrowUp"))
			.hoverImage(DoaSprites.getSprite("arrowUpHover"))
			.pressImage(DoaSprites.getSprite("arrowUpPress"))
			.action(source -> incrementTroopCount())
			.build();
		incrementButton.setPosition(INCREMENT_POSITION);
		addElement(incrementButton);

		centerPieceButton = RoyImageButton.builder()
			.image(DoaSprites.getSprite("centerPiece"))
			.hoverImage(DoaSprites.getSprite("centerPieceHover"))
			.pressImage(DoaSprites.getSprite("centerPiecePress"))
			.disabledImage(DoaSprites.getSprite("centerPieceDisabled"))
			.action(source -> {
				updateSpinnerValues();
				ProvinceHitAreas areas = context.getAreas();
				switch(context.getCurrentPhase()) {
					case DRAFT:
						ProvinceHitArea selectedProvinceHitArea = areas.getSelectedProvince();
						if (selectedProvinceHitArea == null) { return; }
						IProvince selectedProvince = selectedProvinceHitArea.getProvince();
						Deploy deploy = context.setUpDeploy(selectedProvince, selectedTroopCount);
						if (context.applyDeployResult(deploy.calculateResult())) {
							if (context.getRemainingDeploys() == 0) {
								nextPhaseButton.setEnabled(true);
							}
						}
						break;
					case ATTACK_DEPLOY:
						ProvinceHitArea attackerProvinceHitArea = areas.getAttackerProvince();
						ProvinceHitArea defenderProvinceHitArea = areas.getDefenderProvince();
						if (attackerProvinceHitArea == null  || defenderProvinceHitArea == null) { return; }

						IProvince attacker = attackerProvinceHitArea.getProvince();
						IProvince defender = defenderProvinceHitArea.getProvince();

						{ /* to avoid local variable bleeding */
							Reinforce reinforce = context.setUpReinforce(attacker, defender, selectedTroopCount);
							if (context.applyReinforceResult(reinforce.calculateResult())) {
								maxTroopCount = Globals.UNKNOWN_TROOP_COUNT;
								selectedTroopCount = Globals.UNKNOWN_TROOP_COUNT;
								nextPhaseButton.setEnabled(true);
							}
						}
						break;
					case REINFORCE:
						ProvinceHitArea reinforcerProvinceHitArea = areas.getReinforcingProvince();
						ProvinceHitArea reinforceeProvinceHitArea = areas.getReinforceeProvince();
						if (reinforcerProvinceHitArea == null  || reinforceeProvinceHitArea == null) { return; }

						IProvince reinforcer = reinforcerProvinceHitArea.getProvince();
						IProvince reinforcee = reinforceeProvinceHitArea.getProvince();
						if (!reinforcer.canReinforceAnotherProvince()) { return; }

						{ /* to avoid local variable bleeding */
							Reinforce reinforce = context.setUpReinforce(reinforcer, reinforcee, selectedTroopCount);
							if (context.applyReinforceResult(reinforce.calculateResult())) {
								maxTroopCount = Globals.UNKNOWN_TROOP_COUNT;
								selectedTroopCount = Globals.UNKNOWN_TROOP_COUNT;
								nextPhaseButton.setEnabled(false);
							}
						}
						break;
					default:
						break;
				}
				updateSpinnerValues();
			})
			.build();
		centerPieceButton.setPosition(CENTER_PIECE_POSITION);
		centerPieceButton.setTextColor(Color.BLACK);
		centerPieceButton.setHoverTextColor(UIConstants.getTextColor());
		addElement(centerPieceButton);

		updateSpinnerValues();
		if (context.getCurrentPhase() == TurnPhase.SETUP) {
			centerPieceButton.setEnabled(false);
		} else if (context.getCurrentPhase() == TurnPhase.DRAFT) {
			centerPieceButton.setEnabled(true);
		}
		nextPhaseButton.setEnabled(false);

		setzOrder(ZOrders.GAME_UI_Z);

		Renderer r = new Renderer();
		addComponent(new Script(r));
		addComponent(r);
	}

	@RequiredArgsConstructor
	private final class Script extends DoaScript {

		int counter = Globals.DEFAULT_TIME_SLICE;
		private TurnPhase previousPhase;

		@NonNull private Renderer renderer;

		@Override
		public void tick() {
			if(!isVisible()) { return; }

			if (counter < Globals.DEFAULT_TIME_SLICE) {
				counter++;
				return;
			}

			TurnPhase currentPhase = context.getCurrentPhase();
			if (currentPhase != previousPhase) {
				previousPhase = currentPhase;
				updateSpinnerValues();

				//currentPhaseText = Translator.getInstance().getTranslatedString(currentPhase.toString());
				currentPhaseText = currentPhase.toString();
				renderer.recalculateFonts();
			}

			/* Reinforce logic */
			IProvince reinforcer = null;
			if (currentPhase == TurnPhase.REINFORCE) {
				ProvinceHitAreas areas = context.getAreas();
				ProvinceHitArea reinforcerProvinceHitArea = areas.getReinforcingProvince();
				ProvinceHitArea reinforceeProvinceHitArea = areas.getReinforceeProvince();
				if (reinforcerProvinceHitArea != null &&
					reinforceeProvinceHitArea != null &&
					reinforcerProvinceHitArea.getProvince().canReinforceAnotherProvince()) {
					reinforcer = reinforcerProvinceHitArea.getProvince();
				}
				updateSpinnerValuesForReinfocing(reinforcer);
			}
			/* Reinforce logic */

			clickedProvince = context.getAreas().getSelectedProvince() != null ? (Province) context.getAreas().getSelectedProvince().getProvince() : null;
			if (clickedProvince != null) {
				garrisonText = clickedProvince.getNumberOfTroops() == Globals.UNKNOWN_TROOP_COUNT ?
					"???" :
					Integer.toString(clickedProvince.getNumberOfTroops());
				if (clickedProvince.isOccupied()) {
					ownerText = clickedProvince.getOccupier().getName();
				} else {
					ownerText = "";
				}
				nameText = clickedProvince.getName().toUpperCase(Translator.getInstance().getCurrentLanguage().getLocale());
				continentText = clickedProvince.getContinent().getName().toUpperCase(Translator.getInstance().getCurrentLanguage().getLocale());
				renderer.recalculateFonts();
			} else {
				garrisonText = "";
				ownerText = "";
				nameText = "";
				continentText = "";
			}
			counter = 0;
		}
	}

	private final class Renderer extends DoaRenderer {

		private float paddingMultiplier					= 0.9f;

		private transient BufferedImage bottomRing		= DoaSprites.getSprite("MainMenuBottomRing");

		private final BufferedImage MIDDLE				= DoaSprites.getSprite("gaugeBig");
		private final BufferedImage LEFT				= DoaSprites.getSprite("gaugeLeft");
		private final BufferedImage RIGHT				= DoaSprites.getSprite("gaugeRight");

		private transient BufferedImage garrisonBG		= DoaSprites.getSprite("garrisonHolder");
		private transient BufferedImage ownerBG			= DoaSprites.getSprite("ownerHolder");
		private transient BufferedImage provinceBG		= DoaSprites.getSprite("provinceNameHolder");
		private transient BufferedImage continentBG		= DoaSprites.getSprite("continentHolder");

		private transient BufferedImage garrisonIcon	= DoaSprites.getSprite("garrisonHolderIcon");
		private transient BufferedImage ownerIcon		= DoaSprites.getSprite("ownerHolderIcon");
		private transient BufferedImage provinceIcon	= DoaSprites.getSprite("provinceNameHolderIcon");
		private transient BufferedImage continentIcon	= DoaSprites.getSprite("continentHolderIcon");

		private final DoaVector GARRISON_BG_POSITION	= new DoaVector(870, 890);
		private final DoaVector OWNER_BG_POSITION		= new DoaVector(857, 932);
		private final DoaVector PROVINCE_BG_POSITION	= new DoaVector(837, 974);
		private final DoaVector CONTINENT_BG_POSITION	= new DoaVector(825, 1016);

		private  DoaVector PHASE_AREA				= new DoaVector(140, 1400);

		private Font garrisonFont;
		private float garrisonTextWidth;
		private float garrisonTextHeight;

		private Font ownerFont;
		private float ownerTextWidth;
		private float ownerTextHeight;

		private Font provinceNameFont;
		private float provinceTextWidth;
		private float provinceTextHeight;

		private Font continentNameFont;
		private float continentTextWidth;
		private float continentTextHeight;

		private Font turnPhaseFont;

		@Override
		public void render() {
			if(!isVisible()) { return; }

			if (garrisonFont == null) {
				DoaVector contentSize = new DoaVector(garrisonBG.getWidth() * paddingMultiplier, garrisonBG.getHeight() * paddingMultiplier);
				garrisonFont = UIUtils.adjustFontToFitInArea(garrisonText, contentSize);

				garrisonTextWidth = UIUtils.textWidth(garrisonFont, garrisonText);
				garrisonTextHeight = UIUtils.textHeight(garrisonFont);

				float[] strSize = DoaGraphicsFunctions.unwarp(garrisonTextWidth, garrisonTextHeight);
				garrisonTextWidth = strSize[0];
				garrisonTextHeight = strSize[1];
			}
			if (ownerFont == null) {
				DoaVector contentSize = new DoaVector(ownerBG.getWidth() * paddingMultiplier, ownerBG.getHeight() * paddingMultiplier);
				ownerFont = UIUtils.adjustFontToFitInArea(ownerText, contentSize);

				ownerTextWidth = UIUtils.textWidth(ownerFont, ownerText);
				ownerTextHeight = UIUtils.textHeight(ownerFont);

				float[] strSize = DoaGraphicsFunctions.unwarp(ownerTextWidth, ownerTextHeight);
				ownerTextWidth = strSize[0];
				ownerTextHeight = strSize[1];
			}
			if (provinceNameFont == null) {
				DoaVector contentSize = new DoaVector(provinceBG.getWidth() * paddingMultiplier, provinceBG.getHeight() * paddingMultiplier);
				provinceNameFont = UIUtils.adjustFontToFitInArea(nameText, contentSize);

				provinceTextWidth = UIUtils.textWidth(provinceNameFont, nameText);
				provinceTextHeight = UIUtils.textHeight(provinceNameFont);

				float[] strSize = DoaGraphicsFunctions.unwarp(provinceTextWidth, provinceTextHeight);
				provinceTextWidth = strSize[0];
				provinceTextHeight = strSize[1];
			}
			if (continentNameFont == null) {
				DoaVector contentSize = new DoaVector(continentBG.getWidth() * paddingMultiplier, continentBG.getHeight() * paddingMultiplier);
				continentNameFont = UIUtils.adjustFontToFitInArea(continentText, contentSize);

				continentTextWidth = UIUtils.textWidth(continentNameFont, continentText);
				continentTextHeight = UIUtils.textHeight(continentNameFont);

				float[] strSize = DoaGraphicsFunctions.unwarp(continentTextWidth, continentTextHeight);
				continentTextWidth = strSize[0];
				continentTextHeight = strSize[1];
			}
			if (turnPhaseFont == null) {
				DoaVector contentSize = new DoaVector(PHASE_AREA.x, PHASE_AREA.y);
				turnPhaseFont = UIUtils.adjustFontToFitInArea(currentPhaseText, contentSize);
			}

			DoaGraphicsFunctions.setColor(UIConstants.getTextColor());

			DoaGraphicsFunctions.drawImage(bottomRing,
				0, 1080 - bottomRing.getHeight(),
				bottomRing.getWidth(), bottomRing.getHeight()
			);

			DoaGraphicsFunctions.drawImage(LEFT,
				1920 * 0.304f, 1080 - LEFT.getHeight(),
				LEFT.getWidth(), LEFT.getHeight()
			);
			DoaGraphicsFunctions.drawImage(RIGHT,
				1920 * 0.585f, 1080 - RIGHT.getHeight(),
				RIGHT.getWidth(), RIGHT.getHeight()
			);

			DoaGraphicsFunctions.setFont(turnPhaseFont);
			DoaGraphicsFunctions.drawString(
				currentPhaseText,
				nextPhaseButton.getContentArea().getBounds().x + nextPhaseButton.getContentArea().getBounds().width / 2f - 60,
				1072
			);

			DoaGraphicsFunctions.drawImage(MIDDLE,
				(1920 - MIDDLE.getWidth()) / 2f, 1080 - MIDDLE.getHeight(),
				MIDDLE.getWidth(), MIDDLE.getHeight()
			);

			DoaGraphicsFunctions.drawImage(garrisonBG,
				GARRISON_BG_POSITION.x, GARRISON_BG_POSITION.y,
				garrisonBG.getWidth(), garrisonBG.getHeight()
			);
			DoaGraphicsFunctions.drawImage(garrisonIcon,
				GARRISON_BG_POSITION.x + garrisonBG.getWidth() + 6, GARRISON_BG_POSITION.y + 8,
				garrisonIcon.getWidth(), garrisonIcon.getHeight()
			);

			DoaGraphicsFunctions.drawImage(ownerBG,
				OWNER_BG_POSITION.x, OWNER_BG_POSITION.y,
				ownerBG.getWidth(), ownerBG.getHeight()
			);
			DoaGraphicsFunctions.drawImage(ownerIcon,
				OWNER_BG_POSITION.x + ownerBG.getWidth() + 6, OWNER_BG_POSITION.y + 8,
				ownerIcon.getWidth(), ownerIcon.getHeight()
			);

			DoaGraphicsFunctions.drawImage(provinceBG,
				PROVINCE_BG_POSITION.x, PROVINCE_BG_POSITION.y,
				provinceBG.getWidth(), provinceBG.getHeight()
			);
			DoaGraphicsFunctions.drawImage(provinceIcon,
				PROVINCE_BG_POSITION.x + provinceBG.getWidth() + 6, PROVINCE_BG_POSITION.y + 8,
				provinceIcon.getWidth(), provinceIcon.getHeight()
			);

			DoaGraphicsFunctions.drawImage(continentBG,
				CONTINENT_BG_POSITION.x, CONTINENT_BG_POSITION.y,
				continentBG.getWidth(), continentBG.getHeight()
			);
			DoaGraphicsFunctions.drawImage(continentIcon,
				CONTINENT_BG_POSITION.x + continentBG.getWidth() + 6, CONTINENT_BG_POSITION.y + 4,
				continentIcon.getWidth(), continentIcon.getHeight()
			);

			DoaGraphicsFunctions.setFont(garrisonFont);
			DoaGraphicsFunctions.drawString(garrisonText,
				GARRISON_BG_POSITION.x + (garrisonBG.getWidth() - garrisonTextWidth) / 2f,
				GARRISON_BG_POSITION.y + garrisonBG.getHeight() / 2f + garrisonTextHeight / 4f
			);

			DoaGraphicsFunctions.setFont(ownerFont);
			if (clickedProvince != null) {
				DoaGraphicsFunctions.setColor(clickedProvince.getOccupier().getColor());
			}
			DoaGraphicsFunctions.drawString(
				ownerText,
				OWNER_BG_POSITION.x + (ownerBG.getWidth() - ownerTextWidth) / 2f,
				OWNER_BG_POSITION.y + ownerBG.getHeight() / 2f + ownerTextHeight / 4f
			);

			DoaGraphicsFunctions.setFont(provinceNameFont);
			DoaGraphicsFunctions.setColor(UIConstants.getTextColor());
			DoaGraphicsFunctions.drawString(
				nameText,
				PROVINCE_BG_POSITION.x + (provinceBG.getWidth() - provinceTextWidth) / 2f,
				PROVINCE_BG_POSITION.y + provinceBG.getHeight() / 2f + provinceTextHeight / 4f
			);

			DoaGraphicsFunctions.setFont(continentNameFont);
			DoaGraphicsFunctions.drawString(
				continentText,
				CONTINENT_BG_POSITION.x + (continentBG.getWidth() - continentTextWidth) / 2f,
				CONTINENT_BG_POSITION.y + continentBG.getHeight() / 2f + continentTextHeight / 4f
			);
		}

		public void recalculateFonts() {
			garrisonFont = null;
			ownerFont = null;
			provinceNameFont = null;
			continentNameFont = null;
			turnPhaseFont = null;
		}
	}

	private void updateSpinnerValues() {
		maxTroopCount = context.getRemainingDeploys();
		if (selectedTroopCount == Globals.UNKNOWN_TROOP_COUNT || selectedTroopCount == 0) {
			selectedTroopCount = maxTroopCount;
		} else {
			selectedTroopCount = (int) DoaMath.clamp(selectedTroopCount, 0, maxTroopCount);
		}
		if (maxTroopCount != Globals.UNKNOWN_TROOP_COUNT && maxTroopCount != 0) {
			centerPieceButton.setEnabled(true);
			centerPieceButton.setText(Integer.toString(selectedTroopCount));
			nextPhaseButton.setEnabled(false);
		} else {
			centerPieceButton.setEnabled(false);
			centerPieceButton.setText("");
		}
	}

	private void updateSpinnerValuesForReinfocing(IProvince reinforcer) {
		if (reinforcer == null) {
			maxTroopCount = Globals.UNKNOWN_TROOP_COUNT;
			selectedTroopCount = Globals.UNKNOWN_TROOP_COUNT;
			centerPieceButton.setEnabled(false);
			centerPieceButton.setText("");
		} else {
			maxTroopCount = reinforcer.getNumberOfTroops() - 1;
			selectedTroopCount = maxTroopCount;
			centerPieceButton.setEnabled(true);
			centerPieceButton.setText(Integer.toString(selectedTroopCount));
		}
	}

	private void incrementTroopCount() {
		selectedTroopCount = Math.min(selectedTroopCount + 1, maxTroopCount);
		if (centerPieceButton.isEnabled()) {
			centerPieceButton.setText(Integer.toString(selectedTroopCount));
		}
	}

	private void decrementTroopCount() {
		selectedTroopCount = Math.max(selectedTroopCount - 1, 1);
		if (centerPieceButton.isEnabled()) {
			centerPieceButton.setText(Integer.toString(selectedTroopCount));
		}
	}
}
