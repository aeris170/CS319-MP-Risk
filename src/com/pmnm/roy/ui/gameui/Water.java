package com.pmnm.roy.ui.gameui;

import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import com.pmnm.risk.globals.ZOrders;
import com.pmnm.risk.main.Main;

import doa.engine.core.DoaGraphicsFunctions;
import doa.engine.graphics.DoaSprites;
import doa.engine.input.DoaKeyboard;
import doa.engine.scene.DoaObject;
import doa.engine.scene.elements.renderers.DoaRenderer;
import doa.engine.scene.elements.scripts.DoaScript;
import doa.engine.task.DoaTaskGuard;
import doa.engine.task.DoaTasker;
import pmnm.risk.game.databasedimpl.RiskGameContext;

@SuppressWarnings("serial")
public class Water extends DoaObject {

	private static final int SEG_X = 16;
	private static final int SEG_Y = 9;

	private BufferedImage tex = null;
	private BufferedImage bigWinter = new BufferedImage(Main.WINDOW_WIDTH, Main.WINDOW_HEIGHT, BufferedImage.TYPE_INT_ARGB);
	private BufferedImage bigSpring = new BufferedImage(Main.WINDOW_WIDTH, Main.WINDOW_HEIGHT, BufferedImage.TYPE_INT_ARGB);
	private BufferedImage bigSummer = new BufferedImage(Main.WINDOW_WIDTH, Main.WINDOW_HEIGHT, BufferedImage.TYPE_INT_ARGB);
	private BufferedImage bigFall = new BufferedImage(Main.WINDOW_WIDTH, Main.WINDOW_HEIGHT, BufferedImage.TYPE_INT_ARGB);
	BufferedImage currentWaterTexCache = new BufferedImage(Main.WINDOW_WIDTH, Main.WINDOW_HEIGHT, BufferedImage.TYPE_INT_ARGB);

	private DoaTaskGuard renderGuard = new DoaTaskGuard();

	private transient Point2D[][] points = new Point2D.Double[SEG_X][SEG_Y];
	private long[][] startTime = new long[SEG_X][SEG_Y];
	private double[][] intensity = new double[SEG_X][SEG_Y];

	private transient List<TriangularSurface> mesh = new ArrayList<>();

	public Water(RiskGameContext context) {
		for (int y = 0; y < points[0].length; y++) {
			for (int x = 0; x < points.length; x++) {
				points[x][y] = new Point2D.Double(x * Main.WINDOW_WIDTH / (SEG_X - 1f), y * Main.WINDOW_HEIGHT / (SEG_Y - 1f));
				startTime[x][y] = ThreadLocalRandom.current().nextLong();
				intensity[x][y] = ThreadLocalRandom.current().nextInt(1, 10);
			}
		}
		for (int y = 0; y < points[0].length - 1; y++) {
			for (int x = 0; x < points.length - 1; x++) {
				Point2D pa1 = points[x][y];
				Point2D pa2 = points[x + 1][y];
				Point2D pa3 = points[x][y + 1];
				Point2D pb1 = points[x + 1][y];
				Point2D pb2 = points[x][y + 1];
				Point2D pb3 = points[x + 1][y + 1];
				mesh.add(new TriangularSurface(pa1, pa2, pa3));
				mesh.add(new TriangularSurface(pb1, pb2, pb3));
			}
		}
		BufferedImage winterSpr = DoaSprites.getSprite("winterTex");
		BufferedImage springSpr = DoaSprites.getSprite("springTex");
		BufferedImage summerSpr = DoaSprites.getSprite("summerTex");
		BufferedImage fallSpr = DoaSprites.getSprite("springTex");
		winterSpr.setAccelerationPriority(1);
		springSpr.setAccelerationPriority(1);
		summerSpr.setAccelerationPriority(1);
		fallSpr.setAccelerationPriority(1);
		currentWaterTexCache.setAccelerationPriority(1);
		Graphics2D bigWinterRenderer = bigWinter.createGraphics();
		Graphics2D bigSpringRenderer = bigSpring.createGraphics();
		Graphics2D bigSummerRenderer = bigSummer.createGraphics();
		Graphics2D bigFallRenderer = bigFall.createGraphics();
		int texW = winterSpr.getWidth() / 8;
		int texH = winterSpr.getHeight() / 8;
		for (int i = -50; i < Main.WINDOW_WIDTH + 50; i += texW) {
			for (int j = -50; j < Main.WINDOW_HEIGHT + 50; j += texH) {
				bigWinterRenderer.drawImage(winterSpr, i, j, texW, texH, null);
				bigSpringRenderer.drawImage(springSpr, i, j, texW, texH, null);
				bigSummerRenderer.drawImage(summerSpr, i, j, texW, texH, null);
				bigFallRenderer.drawImage(fallSpr, i, j, texW, texH, null);
			}
		}
		bigWinterRenderer.dispose();
		bigSpringRenderer.dispose();
		bigSummerRenderer.dispose();
		bigFallRenderer.dispose();

		setzOrder(ZOrders.WATER_Z);
		addComponent(new Script(context));
		addComponent(new Renderer());
	}

	private final class Script extends DoaScript {

		private RiskGameContext context;
		private long elapsedTime;
		private long previousTime;

		private Script(RiskGameContext context) {
			this.context = context;
			elapsedTime = 0;
			previousTime = System.nanoTime();
		}

		@Override
		public void tick() {
			if (!context.isPaused()) {
				elapsedTime +=  System.nanoTime() - previousTime;

				for (int x = 0; x < points.length; x++) {
					for (int y = 0; y < points[0].length; y++) {
						Point2D p = points[x][y];
						double px = x * Main.WINDOW_WIDTH / (SEG_X - 1f) + (intensity[x][y]) * Math.sin((startTime[x][y] + elapsedTime) * 0.00000000173);
						double py = y * Main.WINDOW_HEIGHT / (SEG_Y - 1f) + (intensity[x][y]) * Math.cos((startTime[x][y] + elapsedTime) * 0.00000000091);
						p.setLocation(px, py);
					}
				}
			}
			previousTime = System.nanoTime();
		}
	}

	private final class Renderer extends DoaRenderer {
		@Override
		public void render() {
			DoaTasker.guardExecution(() -> {
				switch (Season.getCurrentSeason()) {
					case WINTER:
						tex = bigWinter;
						break;
					case SPRING:
						tex = bigSpring;
						break;
					case SUMMER:
						tex = bigSummer;
						break;
					case FALL:
						tex = bigFall;
						break;
					default:
						throw new RuntimeException("Should not reach here!");
				}
				mesh.parallelStream().forEach(TriangularSurface::render);
			}, renderGuard, 75);
			if (DoaKeyboard.NUM_1) {
				Season.currentSeason = Season.FALL;
			}
			if (DoaKeyboard.NUM_2) {
				Season.currentSeason = Season.WINTER;
			}
			if (DoaKeyboard.NUM_3) {
				Season.currentSeason = Season.SPRING;
			}
			if (DoaKeyboard.NUM_4) {
				Season.currentSeason = Season.SUMMER;
			}
			DoaGraphicsFunctions.drawImage(currentWaterTexCache, 0, 0);
		}
	}

	private class TriangularSurface {

		private Point2D[] trianglePoints;
		private Polygon polygon = new Polygon();
		private AffineTransform t1 = new AffineTransform();
		private AffineTransform t2 = new AffineTransform();

		TriangularSurface(Point2D first, Point2D second, Point2D third) {
			trianglePoints = new Point2D[] { first, second, third };
			t1.setTransform(first.getX() - third.getX(), first.getY() - third.getY(), second.getX() - third.getX(), second.getY() - third.getY(), third.getX(), third.getY());
			try {
				t1.invert();
			} catch (NoninvertibleTransformException ex) {
				ex.printStackTrace();
			}
		}

		public void render() {
			Graphics2D g2d = currentWaterTexCache.createGraphics();
			polygon.reset();
			for (Point2D p : trianglePoints) {
				polygon.addPoint((int) p.getX(), (int) p.getY());
			}
			/*
			!!
			if (debugRender) {
				g2d.setColor(java.awt.Color.MAGENTA);
				g2d.setStroke(new java.awt.BasicStroke(2));
				g2d.draw(polygon);
			}
			*/
			g2d.setClip(polygon);
			Point2D a = trianglePoints[0];
			Point2D b = trianglePoints[1];
			Point2D c = trianglePoints[2];
			t2.setTransform(a.getX() - c.getX(), a.getY() - c.getY(), b.getX() - c.getX(), b.getY() - c.getY(), c.getX(), c.getY());
			t2.concatenate(t1);
			g2d.drawImage(tex, t2, null);
			g2d.dispose();
		}
	}
}
