package com.pmnm.risk.map.board;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;
import com.pmnm.risk.dice.Dice;
import com.pmnm.risk.globals.Globals;
import com.pmnm.risk.main.Conflict;
import com.pmnm.risk.main.IPlayer;
import com.pmnm.risk.main.Conflict.Result;
import com.pmnm.risk.main.Deploy;
import com.pmnm.risk.map.ContinentData;
import com.pmnm.risk.map.MapConfig;
import com.pmnm.risk.map.MapData;
import com.pmnm.risk.map.ProvinceData;
import com.pmnm.risk.map.continent.Continent;

import lombok.NonNull;

public class RiskGameContext implements Serializable {

	private static final long serialVersionUID = -7180240760865875029L;

	public static RiskGameContext of(final MapData data) {
		RiskGameContext gameContext = new RiskGameContext(data);
		return gameContext;
	}
	
	private MapData map;
	private IPlayer[] players;

	/* Data <-> Implementation Association */
	private Map<@NonNull IContinent, @NonNull ContinentData> continentData;
	private Map<@NonNull ContinentData, @NonNull IContinent> dataContinent;
	private Map<@NonNull IProvince, @NonNull ProvinceData> provinceData;
	private Map<@NonNull ProvinceData, @NonNull IProvince> dataProvince;
	
	/* Continent <-> Province Association */
	private Map<@NonNull IContinent, @NonNull ImmutableList<@NonNull IProvince>> continentProvinces;
	private Map<@NonNull IProvince, @NonNull IContinent> provinceContinents;
	
	/* Player <-> Province Association */
	private Map<@NonNull IPlayer, @NonNull ArrayList<@NonNull IProvince>> playerProvinces;
	private Map<@NonNull IProvince, @NonNull IPlayer> provincePlayers;
	
	/* Province Runtime info */
	private Map<@NonNull IProvince, @NonNull ImmutableList<@NonNull IProvince>> neighbors;
	private Map<@NonNull IProvince, @NonNull Integer> numberOfTroops;
	
	/* Province Runtime info */
	private IPlayer currentPlayingPlayer;
	
	private RiskGameContext(@NonNull final MapData data) {
		map = data;

		/* --- Step 1, create IContinents and associate them with their data --- */
		continentData = new HashMap<>();
		dataContinent = new HashMap<>();
		UnmodifiableIterator<@NonNull ContinentData> continentDatas = map.getContinents();
		while (continentDatas.hasNext()) {
			ContinentData cData = continentDatas.next();
			IContinent continent = new Continent(this, cData);
			continentData.put(continent, cData);
			dataContinent.put(cData, continent);
		}
		/* --------------------------------------------------------------------- */ 

		/* --------------- Step 2, do Step 1 but for IProvinces ---------------- */
		provinceData = new HashMap<>();
		dataProvince = new HashMap<>();
		continentDatas = map.getContinents();
		while (continentDatas.hasNext()) {
			ContinentData cData = continentDatas.next();
			UnmodifiableIterator<@NonNull ProvinceData> provinceDatas = cData.getProvinces();
			
			while (provinceDatas.hasNext()) {
				ProvinceData pData = provinceDatas.next();
				IProvince province = new Province(this, pData);
				provinceData.put(province, pData);
				dataProvince.put(pData, province);
			}
		}		
		/* --------------------------------------------------------------------- */ 

		/* ------ Step 3, data are realized sort provinces into continents ----- */
		continentProvinces = new HashMap<>();
		provinceContinents = new HashMap<>();
		dataContinent.keySet().forEach(cData -> {
			List<@NonNull IProvince> provinces = new ArrayList<>();
			
			UnmodifiableIterator<@NonNull ProvinceData> provinceDatas = cData.getProvinces();
			while (provinceDatas.hasNext()) {
				ProvinceData pData = provinceDatas.next();
				provinces.add(objectOf(pData));
				provinceContinents.put(objectOf(pData), objectOf(cData));
			}
			
			continentProvinces.put(objectOf(cData), ImmutableList.copyOf(provinces));
		});
		/* --------------------------------------------------------------------- */ 

		/* ------------------ Step 4, set player associations ------------------ */
		playerProvinces = new HashMap<>();
		provincePlayers = new HashMap<>();
		
		/* --------------------------------------------------------------------- */ 

		/* ----------------- Step 5, set neigbors of provinces ----------------- */
		neighbors = new HashMap<>();
		dataProvince.keySet().forEach(pData -> {
			List<@NonNull IProvince> provinces = new ArrayList<>();
			
			UnmodifiableIterator<@NonNull ProvinceData> neighborDatas = pData.getNeighbors();
			while (neighborDatas.hasNext()) {
				ProvinceData nData = neighborDatas.next();
				provinces.add(objectOf(nData));
			}
			
			neighbors.put(objectOf(pData), ImmutableList.copyOf(provinces));
		});
		/* --------------------------------------------------------------------- */ 

		/* ----------------- Step 6, add provinces to this map ----------------- */
		numberOfTroops = new HashMap<>();
		provinceData.keySet().forEach(province -> {
			numberOfTroops.put(province, Globals.UNKNOWN_TROOP_COUNT);
		});
		/* --------------------------------------------------------------------- */ 
	}
	
	/* Game API */
	public Conflict setUpConflict(@NonNull final IProvince attacker, @NonNull final IProvince defender, @NonNull final Dice attackerDice) {
		return new Conflict(this, attacker, defender, attackerDice);
	}
	public void applyConflictResult(@NonNull final Conflict.Result result) {
		Conflict conflict = result.getConflict();
		
		IProvince attacker = conflict.getAttacker();
		numberOfTroops.put(attacker, result.getRemainingAttackerTroops());
		
		IProvince defender = conflict.getDefender();
		numberOfTroops.put(defender, result.getRemainingDefenderTroops());
		
		if(numberOfTroops.get(defender) <= 0) {
			IPlayer attackerPlayer = provincePlayers.get(attacker);
			IPlayer defenderPlayer = provincePlayers.get(defender);
			
			playerProvinces.get(defenderPlayer).remove(defender);
			provincePlayers.put(defender, attackerPlayer);
			
			numberOfTroops.put(defender, Globals.UNKNOWN_TROOP_COUNT);
		}
	}
	public Deploy setUpDeploy(@NonNull final IProvince source, @NonNull final IProvince defender, int amount) {
		return new Deploy(this, source, defender, amount);
	}
	public void applyDeployResult(@NonNull final Deploy.Result result) {
		Deploy deploy = result.getDeploy();
		
		IProvince deployer = deploy.getSource();
		numberOfTroops.put(deployer, result.getRemainingSourceTroops());

		IProvince deployee = deploy.getDestination();
		numberOfTroops.put(deployee, result.getRemainingDestinationTroops());
	}
	
	/* Province API */
	public IContinent continentOf(@NonNull final IProvince province) {
		return provinceContinents.get(province);
	}
	public boolean hasOccupier(@NonNull final IProvince province) {
		return occupierOf(province) != null;
	}
	public IPlayer occupierOf(@NonNull final IProvince province) {
		return provincePlayers.get(province);
	}
	public UnmodifiableIterator<@NonNull IProvince> neighborsOf(@NonNull final IProvince province) {
		return neighbors.get(province).iterator();
	}
	public int numberOfTroopsOn(@NonNull final IProvince province) {
		return numberOfTroops.get(province);
	}
	
	/* Continent API */
	public UnmodifiableIterator<@NonNull IProvince> provincesOf(@NonNull final IContinent continent) {
		return continentProvinces.get(continent).iterator();
	}
	
	/* Private Getters */
	private ProvinceData dataOf(@NonNull final IProvince province) {
		return provinceData.get(province);
	}
	private ContinentData dataOf(@NonNull final IContinent continent) {
		return continentData.get(continent);
	}
	private IProvince objectOf(@NonNull final ProvinceData data) {
		return dataProvince.get(data);
	}
	private IContinent objectOf(@NonNull final ContinentData data) {
		return dataContinent.get(data);
	}
}