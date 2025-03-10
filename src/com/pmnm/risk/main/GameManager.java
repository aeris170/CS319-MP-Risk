package com.pmnm.risk.main;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import com.pmnm.risk.globals.Builders;
import com.pmnm.risk.globals.Scenes;
import com.pmnm.risk.toolkit.Utils;
import com.pmnm.roy.ui.gameui.BottomPanel;
import com.pmnm.roy.ui.gameui.DicePanel;
import com.pmnm.roy.ui.gameui.RiskGameScreenUI;

import doa.engine.graphics.DoaGraphicsContext;
import doa.engine.graphics.DoaSprites;
import doa.engine.input.DoaMouse;
import doa.engine.scene.DoaObject;
import pmnm.risk.game.Dice;
import pmnm.risk.game.IRiskGameContext.TurnPhase;
import pmnm.risk.game.databasedimpl.Player;
import pmnm.risk.game.databasedimpl.Province;
import pmnm.risk.map.board.ProvinceConnector;
import pmnm.risk.map.board.ProvinceHitArea;

public class GameManager extends DoaObject {

	private static final long serialVersionUID = -4928417050440420799L;

	public static GameManager INSTANCE;

	public final List<Player> players = new ArrayList<>();

	public int numberOfPlayers;
	public boolean manualPlacement;

	public boolean isManualPlacementDone = false;
	public final Map<Player, Integer> startingTroops = new HashMap<>();
	public int placementCounter = 0;

	public TurnPhase currentPhase = TurnPhase.DRAFT;
	public int reinforcementForThisTurn = 0;
	public Player currentPlayer;
	public int turnCount = 0;

	public ProvinceHitArea moveAfterOccupySource = null;
	public ProvinceHitArea moveAfterOccupyDestination = null;

	public ProvinceHitArea attackerProvinceHitArea = null;
	public ProvinceHitArea defenderProvinceHitArea = null;
	public transient DicePanel dicePanel = RiskGameScreenUI.DicePanel;
	public boolean cardWillBeGiven = false;

	public ProvinceHitArea reinforcingProvince = null;
	public ProvinceHitArea reinforcedProvince = null;

	public ProvinceHitArea clickedHitArea;

	private Province draftReinforceProvince = null;

	public String currentMapName;

	public float timer = 0;

	public boolean isPaused = false;
	public boolean isSinglePlayer = true;

	public GameManager(String mapName, List<Integer> playerTypes, List<String> playerNames, List<Color> playerColors, List<String> aiNames, List<Color> aiColors,
	        List<Integer> difficulties, boolean randomPlacement) {
		super(0f, 0f);
		currentMapName = mapName;
		numberOfPlayers = playerNames.size() + aiNames.size();
		int startingTroopCount = Player.findStartingTroopCount(numberOfPlayers);
		int aiInt = 0;
		int pInt = 0;
		for (int i = 0; i < numberOfPlayers; i++) {
			if (playerTypes.get(i) == 1) {
				Player p = Builders.PB.args(playerNames.get(pInt), playerColors.get(pInt), true).scene(Scenes.GAME_SCENE).instantiate();
				players.add(p);
				startingTroops.put(p, startingTroopCount);
				pInt++;
			} else {
				Player p = Builders.AIPB.args(aiNames.get(aiInt), aiColors.get(aiInt), difficulties.get(aiInt)).scene(Scenes.GAME_SCENE).instantiate();
				players.add(p);
				startingTroops.put(p, startingTroopCount);
				aiInt++;
			}
		}
		manualPlacement = !randomPlacement;

		currentPlayer = players.get(0);
		currentPlayer.turn();
		if (!manualPlacement) {
			randomPlacement();
		}
		// GameInstance.saveCurrentState();
		INSTANCE = this;
	}

	public void nextPhase() {
		if (currentPhase == TurnPhase.DRAFT) {
			currentPhase = TurnPhase.ATTACK;
			BottomPanel.nullSpinner();
		} else if (currentPhase == TurnPhase.ATTACK) {
			currentPhase = TurnPhase.REINFORCE;
			markAttackerProvince(null);
			markDefenderProvince(null);
		} else if (currentPhase == TurnPhase.REINFORCE) {
			currentPhase = TurnPhase.DRAFT;
			currentPlayer.endTurn();
			turnCount++;
			currentPlayer = players.get(turnCount % players.size());
			currentPlayer.turn();
			reinforcementForThisTurn = Player.calculateReinforcementsForThisTurn(currentPlayer);
			markReinforcingProvince(null);
			markReinforcedProvince(null);
			BottomPanel.updateSpinnerValues(1, reinforcementForThisTurn);
			BottomPanel.nextPhaseButton.disable();
			timer = 0;
		}
	}

	@Override
	public void tick() {
		if (isSinglePlayer) {
			if (!isPaused) {
				if (DoaMouse.MB1) {
					clickedHitArea = ProvinceHitArea.ALL_PROVINCE_HIT_AREAS.stream().filter(ProvinceHitArea::isMouseClicked).findFirst().orElse(null);
				}
				if (!isManualPlacementDone) {
					if (startingTroops.values().stream().allMatch(v -> v <= 0)) {
						isManualPlacementDone = true;
						reinforcementForThisTurn = Player.calculateReinforcementsForThisTurn(currentPlayer);
						BottomPanel.updateSpinnerValues(1, reinforcementForThisTurn);
					}
				}
				if (isManualPlacementDone) {
					timer += 0.025f;
				}
				if (timer > (Main.WINDOW_WIDTH - DoaSprites.get("seasonCircle").getWidth()) / 2) {
					currentPhase = TurnPhase.DRAFT;
					currentPlayer.endTurn();
					++turnCount;
					currentPlayer = players.get(turnCount % players.size());
					currentPlayer.turn();
					reinforcementForThisTurn = Player.calculateReinforcementsForThisTurn(currentPlayer);
					markReinforcingProvince(null);
					markReinforcedProvince(null);
					BottomPanel.updateSpinnerValues(1, reinforcementForThisTurn);
					BottomPanel.nextPhaseButton.disable();
					timer = 0;
				}
			}
		} else {/* try { GameInstance.loadLastStateAndCompare(); } catch (ClassNotFoundException | IOException ex) {
		         * ex.printStackTrace(); if (isManualPlacementDone) { timer += 0.1f; } if (timer >
		         * (Main.WINDOW_WIDTH - DoaSprites.get("seasonCircle").getWidth()) / 2) { currentPhase =
		         * TurnPhase.DRAFT; if (cardWillBeGiven) { // currentPlayer.addCard(Card.getRandomCard());
		         * cardWillBeGiven = false; } currentPlayer.endTurn(); ++turnCount; currentPlayer =
		         * players.get(turnCount % players.size()); currentPlayer.turn(); reinforcementForThisTurn =
		         * Player.calculateReinforcementsForThisTurn(currentPlayer); markReinforcingProvince(null);
		         * markReinforcedProvince(null); BottomPanel.updateSpinnerValues(1, reinforcementForThisTurn);
		         * BottomPanel.nextPhaseButton.disable(); timer = 0; } } */
		}
	}

	@Override
	public void render(DoaGraphicsContext g) {}

	public void claimProvince(Province claimed) {
		claimed.getClaimedBy(currentPlayer);
		startingTroops.put(currentPlayer, startingTroops.get(currentPlayer) - 1);
		currentPlayer = players.get(++placementCounter % players.size());
		currentPlayer.turn();
	}

	public void draftReinforce(int reinforcementCount) {
		if (draftReinforceProvince != null) {
			draftReinforceProvince.addTroops(reinforcementCount);
			if (!isManualPlacementDone) {
				startingTroops.put(currentPlayer, startingTroops.get(currentPlayer) - reinforcementCount);
				currentPlayer = players.get(++placementCounter % players.size());
				currentPlayer.turn();
			} else {
				reinforcementForThisTurn -= reinforcementCount;
				if (reinforcementForThisTurn <= 0) {
					nextPhase();
				} else {
					BottomPanel.updateSpinnerValues(1, reinforcementForThisTurn);
				}
			}
			draftReinforceProvince.getProvinceHitArea().isSelected = false;
			draftReinforceProvince = null;
		}
	}

	public int numberOfReinforcementsForThisTurn() {
		return reinforcementForThisTurn;
	}

	public boolean areAllProvincesClaimed() {
		return Province.ALL_PROVINCES.stream().filter(province -> province.isClaimed()).count() == Province.ALL_PROVINCES.size();
	}

	public void markAttackerProvince(ProvinceHitArea province) {
		if (attackerProvinceHitArea != null) {
			ProvinceHitArea.ALL_PROVINCE_HIT_AREAS.stream()
			        .filter(hitArea -> attackerProvinceHitArea.getProvince().getNeighbours().contains(hitArea.getProvince()) && !hitArea.getProvince().isOwnedBy(currentPlayer))
			        .collect(Collectors.toList()).forEach(hitArea -> hitArea.deemphasizeForAttack());
			attackerProvinceHitArea.deselectAsAttacker();
		}
		attackerProvinceHitArea = province;
		if (attackerProvinceHitArea != null) {
			ProvinceHitArea.ALL_PROVINCE_HIT_AREAS.stream()
			        .filter(hitArea -> attackerProvinceHitArea.getProvince().getNeighbours().contains(hitArea.getProvince()) && !hitArea.getProvince().isOwnedBy(currentPlayer))
			        .collect(Collectors.toList()).forEach(hitArea -> hitArea.emphasizeForAttack());
			attackerProvinceHitArea.selectAsAttacker();
		}
	}

	public void markDefenderProvince(ProvinceHitArea province) {
		if (defenderProvinceHitArea != null) {
			defenderProvinceHitArea.deselectAsDefender();
		}
		defenderProvinceHitArea = province;
		if (defenderProvinceHitArea != null) {
			defenderProvinceHitArea.selectAsDefender();
			if (currentPlayer.isLocalPlayer()) {
				dicePanel.show();
			}
		} else {
			if (currentPlayer.isLocalPlayer()) {
				dicePanel.hide();
			}
		}
	}

	public ProvinceHitArea getAttackerProvince() {
		return attackerProvinceHitArea;
	}

	public void toss(int diceAmount) {
		Integer[] attackerDiceValues = null;
		Integer[] defenderDiceValues = null;
		if (defenderProvinceHitArea.getProvince().getTroops() == 1 || diceAmount == 1) {
			defenderDiceValues = Arrays.stream(Dice.DEFENCE_DICE_1.rollAllAndGetAll()).boxed().toArray(Integer[]::new);
		} else {
			defenderDiceValues = Arrays.stream(Dice.DEFENCE_DICE_2.rollAllAndGetAll()).boxed().toArray(Integer[]::new);
		}
		switch (diceAmount) {
			case 1:
				if (attackerProvinceHitArea.getProvince().getTroops() > 1) {
					attackerDiceValues = Arrays.stream(Dice.ATTACK_DICE_1.rollAllAndGetAll()).boxed().toArray(Integer[]::new);
				}
				break;
			case 2:
				if (attackerProvinceHitArea.getProvince().getTroops() > 2) {
					attackerDiceValues = Arrays.stream(Dice.ATTACK_DICE_2.rollAllAndGetAll()).boxed().toArray(Integer[]::new);
				}
				break;
			case 3:
				if (attackerProvinceHitArea.getProvince().getTroops() > 3) {
					attackerDiceValues = Arrays.stream(Dice.ATTACK_DICE_3.rollAllAndGetAll()).boxed().toArray(Integer[]::new);
				}
				break;
			default:
				break;
		}
		if (attackerDiceValues != null) {
			Arrays.sort(attackerDiceValues, Collections.reverseOrder());
			Arrays.sort(defenderDiceValues, Collections.reverseOrder());
			int attackerCasualties = 0;
			int defenderCasualties = 0;
			for (int i = 0; i < Math.min(attackerDiceValues.length, defenderDiceValues.length); i++) {
				if (attackerDiceValues[i] > defenderDiceValues[i]) {
					defenderCasualties++;
				} else {
					attackerCasualties++;
				}
			}
			defenderProvinceHitArea.getProvince().removeTroops(defenderCasualties);
			attackerProvinceHitArea.getProvince().removeTroops(attackerCasualties);
			if (attackerProvinceHitArea.getProvince().getTroops() == 1) {
				markAttackerProvince(null);
				markDefenderProvince(null);
			}
			if (defenderProvinceHitArea != null && defenderProvinceHitArea.getProvince().getTroops() <= 0) {
				// capture
				defenderProvinceHitArea.getProvince().removeTroops(defenderProvinceHitArea.getProvince().getTroops() + 1);
				BottomPanel.updateSpinnerValues(diceAmount, attackerProvinceHitArea.getProvince().getTroops() - 1);
				occupyProvince(defenderProvinceHitArea.getProvince());
			}
		}
	}

	public void blitz() {
		if (attackerProvinceHitArea != null && defenderProvinceHitArea != null) {
			int attackerTroops = attackerProvinceHitArea.getProvince().getTroops();
			if (attackerTroops <= 1 || defenderProvinceHitArea.getProvince().getTroops() <= 0) {
				return;
			}
			switch (attackerTroops) {
				default:
					toss(3);
					break;
				case 3:
					toss(2);
					break;
				case 2:
					toss(1);
					break;
			}
			blitz();
		}
	}

	private void occupyProvince(Province occupied) {
		ProvinceConnector.getInstance().setPath(attackerProvinceHitArea, defenderProvinceHitArea);
		attackerProvinceHitArea.isSelected = false;
		defenderProvinceHitArea.isSelected = false;
		occupied.getOccupiedBy(currentPlayer);
		defenderProvinceHitArea.deemphasizeForAttack();
		moveAfterOccupySource = attackerProvinceHitArea;
		moveAfterOccupyDestination = defenderProvinceHitArea;
		markAttackerProvince(null);
		markDefenderProvince(null);
		BottomPanel.nextPhaseButton.disable();
		cardWillBeGiven = true;
	}

	public ProvinceHitArea getReinforcingProvince() {
		return reinforcingProvince;
	}

	public void markReinforcingProvince(ProvinceHitArea province) {
		if (reinforcingProvince != null) {
			reinforcingProvince.deselectAsReinforcing();
			Utils.connectedComponents(reinforcingProvince).forEach(hitArea -> {
				hitArea.deemphasizeForReinforcement();
			});
		}
		reinforcingProvince = province;
		if (reinforcingProvince != null) {
			reinforcingProvince.selectAsReinforcing();
			Utils.connectedComponents(reinforcingProvince).forEach(hitArea -> {
				hitArea.emphasizeForReinforcement();
			});
		}
	}

	public ProvinceHitArea getReinforcedProvince() {
		return reinforcedProvince;
	}

	public void markReinforcedProvince(ProvinceHitArea province) {
		if (reinforcedProvince != null) {
			reinforcedProvince.deselectAsReinforced();
		}
		reinforcedProvince = province;
		if (reinforcedProvince != null) {
			reinforcedProvince.selectAsReinforced();
			ProvinceConnector.getInstance().setPath(Utils.shortestPath(reinforcingProvince, reinforcedProvince));
			if (currentPlayer.isLocalPlayer()) {
				BottomPanel.updateSpinnerValues(1, reinforcingProvince.getProvince().getTroops() - 1);
			}
		} else {
			BottomPanel.nullSpinner();
			ProvinceConnector.getInstance().setPath();
		}
	}

	public void reinforce(int reinforcementCount) {
		if (reinforcingProvince != null && reinforcedProvince != null) {
			reinforcingProvince.getProvince().removeTroops(reinforcementCount);
			reinforcedProvince.getProvince().addTroops(reinforcementCount);
			Utils.connectedComponents(reinforcingProvince).forEach(p -> p.deemphasizeForReinforcement());
			ProvinceConnector.getInstance().setPath();
			reinforcingProvince.isSelected = false;
			reinforcedProvince.isSelected = false;
			nextPhase();
		}
	}

	private void randomPlacement() {
		while (!Province.UNCLAIMED_PROVINCES.isEmpty()) {
			currentPlayer.endTurn();
			claimProvince(Province.getRandomUnclaimedProvince());
		}
		while (!startingTroops.values().stream().allMatch(v -> v == 0)) {
			List<Province> playerProvinces = Player.getPlayerProvinces(currentPlayer);
			currentPlayer.endTurn();
			draftReinforceProvince = playerProvinces.get(ThreadLocalRandom.current().nextInt(playerProvinces.size()));
			draftReinforce(1);
		}
	}

	public void setDraftReinforceProvince(Province clickedProvince) {
		draftReinforceProvince = clickedProvince;
	}

	public void moveTroopsAfterOccupying(int count) {
		if (moveAfterOccupyDestination != null) {
			// 1 is there because it was -1 before
			moveAfterOccupyDestination.getProvince().addTroops(1 + count);
			moveAfterOccupySource.getProvince().removeTroops(count);
			moveAfterOccupyDestination = null;
			moveAfterOccupySource = null;
			ProvinceConnector.getInstance().setPath();
		}
	}
}