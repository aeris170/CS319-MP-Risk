package pmnm.risk.map.board;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.util.Map;

import com.pmnm.risk.globals.ZOrders;
import com.pmnm.risk.toolkit.Utils;

import doa.engine.core.DoaGraphicsFunctions;
import doa.engine.scene.DoaObject;
import doa.engine.scene.DoaScene;
import doa.engine.scene.elements.renderers.DoaRenderer;
import doa.engine.scene.elements.scripts.DoaScript;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import pmnm.risk.game.IProvince;
import pmnm.risk.map.Mesh2D;
import pmnm.risk.map.Vertex2D;

public class ProvinceHitArea extends DoaObject {

	static ProvinceHitArea of(IProvince province) {
		return new ProvinceHitArea(province);
	}

	private static final long serialVersionUID = -6848368535793292243L;

	@Getter
	@NonNull
	private IProvince province;

	/* MESHES */
	transient BufferedImage unoccupiedMesh;
	transient Map<Color, BufferedImage> playerOwnedMeshes;
	transient BufferedImage selectedMesh;
	transient BufferedImage selectedBorder;
	transient BufferedImage emphasizedBorder;
	transient BufferedImage highlightBorder;
	/* ------ */

	private boolean isPathVisible = true;
	private boolean isPointsVisible = false;

	@Getter(value = AccessLevel.PACKAGE)
	private ProvinceHitAreaBounds bounds;

	private boolean isAttacker 		= false;
	private boolean isDefender 		= false;
	private boolean isEmphasized 	= false;
	@Setter(value = AccessLevel.PACKAGE)
	private boolean isHighlighted 	= false;
	@Setter(value = AccessLevel.PACKAGE)
	private boolean isSelected 		= false;
	private boolean isReinforcing 	= false;
	private boolean isReinforced 	= false;

	private float meshAlpha = 0f;
	private float meshAlphaDelta = 0.005f;

	private ProvinceSymbol symbol;

	private ProvinceHitArea(IProvince province) {
		this.province = province;
		name = province.getName() + " HitArea";

		bounds = ProvinceHitAreaBounds.of(this);
		ProvinceHitAreaCacher.cache(this);

		addComponent(new ZOrderAndAlphaSetter());
		addComponent(new Renderer());

		symbol = ProvinceSymbol.of(this);
	}

	@Override
	public void onAddToScene(DoaScene scene) {
		super.onAddToScene(scene);
		scene.add(symbol);
	}

	@Override
	public void onRemoveFromScene(DoaScene scene) {
		super.onRemoveFromScene(scene);
		scene.remove(symbol);
	}

	public class ZOrderAndAlphaSetter extends DoaScript {

		private static final long serialVersionUID = -7463836870534187697L;

		@Override
		public synchronized void tick() {
			if (isHighlighted) {
				setzOrder(ZOrders.HIGHLIGHTED_PROVINCE_Z);
			} else if (isEmphasized) {
				setzOrder(ZOrders.EMPHASIZED_PROVINCE_Z);
			} else if (isAttacker || isDefender || isReinforcing || isReinforced) {
				setzOrder(ZOrders.MUTATING_PROVINCE_Z);
			} else if (getzOrder() != 1) {
				setzOrder(ZOrders.DEFAULT_PROVINCE_Z);
			}
			if (isSelected) {
				setzOrder(ZOrders.SELECTED_PROVINCE_Z);
				meshAlpha += meshAlphaDelta;
				if (meshAlpha >= 0.6f) {
					meshAlpha = 0.6f;
					meshAlphaDelta *= -1;
				} else if (meshAlpha <= 0f) {
					meshAlpha = 0f;
					meshAlphaDelta *= -1;
				}
			} else {
				meshAlpha = 0f;
				if (getzOrder() == ZOrders.SELECTED_PROVINCE_Z) {
					setzOrder(ZOrders.DEFAULT_PROVINCE_Z);
				}
			}
		}
	}

	public class Renderer extends DoaRenderer {

		private static final long serialVersionUID = 3390229334254057350L;

		private float translateX = bounds.minX - 4f;
		private float translateY = bounds.minY - 4f;

		@Override
		public void render() {
			if (isPathVisible) {
				DoaGraphicsFunctions.translate(translateX, translateY);
				if (province.isOccupied()) {
					BufferedImage prov = playerOwnedMeshes.get(province.getOccupier().getColor());
					DoaGraphicsFunctions.drawImage(prov, 0, 0, prov.getWidth(), prov.getHeight());
					if (isEmphasized) {
						DoaGraphicsFunctions.drawImage(emphasizedBorder, 0, 0, emphasizedBorder.getWidth(), emphasizedBorder.getHeight());
					}
					if (isHighlighted) {
						DoaGraphicsFunctions.drawImage(highlightBorder, 0, 0, highlightBorder.getWidth(), highlightBorder.getHeight());
					}
					if (isAttacker) {
						DoaGraphicsFunctions.drawImage(selectedBorder, 0, 0, selectedBorder.getWidth(), selectedBorder.getHeight());
					}
					if (isDefender) {
						DoaGraphicsFunctions.drawImage(selectedBorder, 0, 0, selectedBorder.getWidth(), selectedBorder.getHeight());
					}
					if (isSelected) {
						DoaGraphicsFunctions.pushComposite();
						DoaGraphicsFunctions.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, meshAlpha));
						DoaGraphicsFunctions.drawImage(selectedMesh, 0, 0, selectedMesh.getWidth(), selectedMesh.getHeight());
						DoaGraphicsFunctions.popComposite();
						DoaGraphicsFunctions.drawImage(highlightBorder, 0, 0, highlightBorder.getWidth(), highlightBorder.getHeight());
					}
				} else {
					DoaGraphicsFunctions.drawImage(unoccupiedMesh, 0, 0, unoccupiedMesh.getWidth(), unoccupiedMesh.getHeight());
				}
				DoaGraphicsFunctions.translate(-translateX, -translateY);
			}
			if (isPointsVisible) {
				DoaGraphicsFunctions.setColor(Color.MAGENTA);
				for (Mesh2D mesh : province.getMeshes()) {
					GeneralPath gp = mesh.getBoundary();
					Vertex2D[] points = Utils.getPointsOf(gp);
					for (int i = 0; i < points.length; i++) {
						DoaGraphicsFunctions.fillRect(points[i].getX() - 1f, points[i].getY() - 1f, 1, 1);
					}
				}
			}
		}
	}

	public void selectAsAttacker() {
		isAttacker = true;
		setzOrder(ZOrders.MUTATING_PROVINCE_Z);
	}

	public void selectAsDefender() {
		isDefender = true;
		setzOrder(ZOrders.MUTATING_PROVINCE_Z);
	}

	public void deselectAsAttacker() {
		isAttacker = false;
		setzOrder(ZOrders.DESELECTED_PROVINCE_Z);
	}

	public void deselectAsDefender() {
		isDefender = false;
		setzOrder(ZOrders.DESELECTED_PROVINCE_Z);
	}

	public void emphasizeForAttack() {
		isEmphasized = true;
		setzOrder(ZOrders.EMPHASIZED_PROVINCE_Z);
	}

	public void deemphasizeForAttack() {
		isEmphasized = false;
		setzOrder(ZOrders.DESELECTED_PROVINCE_Z);
	}

	public void selectAsReinforcing() {
		isReinforcing = true;
		setzOrder(ZOrders.MUTATING_PROVINCE_Z);
	}

	public void selectAsReinforced() {
		isReinforced = true;
		setzOrder(ZOrders.MUTATING_PROVINCE_Z);
	}

	public void deselectAsReinforcing() {
		isReinforcing = false;
		setzOrder(ZOrders.DESELECTED_PROVINCE_Z);
	}

	public void deselectAsReinforced() {
		isReinforced = false;
		setzOrder(ZOrders.DESELECTED_PROVINCE_Z);
	}

	public void emphasizeForReinforcement() {
		isEmphasized = true;
	}

	public void deemphasizeForReinforcement() {
		isEmphasized = false;
	}
}
