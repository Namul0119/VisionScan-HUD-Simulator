package exercise01;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.util.ArrayList;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

public class ArchimedeanSpiralEx extends JPanel {

	//게임 상태 관련 함수
	int score = 0;				//누적 점수
	int timeElapsed = 0;		//경과 시간(초)
	int timeCounter = 0;		//프레임 누적용 카운터
	private Timer timer;
	
	//플레이어 상태
	int playerHP = 100;			//플레이어 체력
	int attackTimer = 0;  		//적 공격 주기
	boolean gameOver = false;
	
	//보스 경고 효과
	int bossWarningFrame = 0;
	double pulseRadius = 0;
	boolean pulseGrowing = true;
	
	//라운드 관리
	int currentround = 1;
	boolean showRoundText = true;
	int roundTextTimer = 0;
	boolean showRoundBanner = false;
	int roundBannerTimer = 0;
	int roundFRame = 0;
	boolean roundInProgress = false;
	
	//HUD 시각 효과
	private double scanAngle = 0;
	private int targetX;
	private int targetY;
	private double zoom = 1.0;
	private boolean zoomIn = true;
	
	//엔티티 목록
	private ArrayList<Target> targets = new ArrayList<>();
	private ArrayList<Missile> missiles = new ArrayList<>();
	private ArrayList<Explosion> explosions = new ArrayList<>();
	private ArrayList<EnemyBullet> enemyBullets = new ArrayList<>();
	
	//탐지 상태
	private double targetAngle;
	private double targetRadius;
	private boolean targetFound = false;
	
	//스캔 효과용 변수
	private boolean blinkOn = true;
	private int blinkCounter = 0;
	private int detectedFrameCount = 0;
	private boolean soundPlayed = false;
	
	//주기적 타이밍 제어
	private int frameCounter = 0;
	
	public ArchimedeanSpiralEx() {
		//HUD 크기 기준 무작위 위치 설정
		int centerX = 400;
		int centerY = 400;
		
		//마우스 클릭 이벤트 등록
		addMouseListener(new java.awt.event.MouseAdapter(){
			@Override
			public void mousePressed(java.awt.event.MouseEvent e) {
				handleClick(e.getX(), e.getY());
			}
		});
		
		//키보드 입력 설정(스페이스바로 미사일 발사)
		setFocusable(true);
		requestFocusInWindow();
		addKeyListener(new java.awt.event.KeyAdapter() {
			@Override
			public void keyPressed(java.awt.event.KeyEvent e) {
				if(e.getKeyCode() == java.awt.event.KeyEvent.VK_SPACE) {
					fireMissile();
				}
			}
		});
		
		//초기 적 생성(첫 시작 시 5명 고정)
		int numTargets = 5;
		for(int i=0; i<numTargets; i++) {
			Target t = new Target();
			t.angle = Math.random() * 2 * Math.PI;
			t.radius = 100 + Math.random() * 180;
			t.name = "Enemy " + (char)('A' + i);  //Enemy A, B, C..
			targets.add(t);
		}		
		
		//초기 타겟 포지션 랜덤
		int maxRadius = 280;
		targetRadius = Math.random() * maxRadius;
		targetAngle = Math.random() * 2 * Math.PI;
		targetX = centerX + (int)(targetRadius * Math.cos(targetAngle));
		targetY = centerY + (int)(targetRadius * Math.sin(targetAngle));
		
		//메인 타이머 설정
		this.timer = new Timer(30, e -> updateGameLogic());
		this.timer.start();
	}
	
	public void updateGameLogic() {
		if(gameOver) return;  //게임 종료되었으면 아무 처리도 하지 않음
		
		//스캔선 회전 + 확대/축소 애니메이션
		scanAngle += 0.05;
		if(zoomIn) {zoom += 0.005;}
		else {zoom -= 0.005;}
		if(zoom > 1.2) {zoomIn = false;}
		if(zoom < 0.9) {zoomIn = true;}
		
		//미사일 이동 및 충돌체크
		ArrayList<Missile> toRemove = new ArrayList<>();
		for(Missile m : missiles) {
			m.distance += m.speed;
			int mx = getWidth()/2 + (int)(m.distance * Math.cos(m.angle));
			int my = getHeight()/2 + (int)(m.distance * Math.sin(m.angle));
			
			for(Target t : targets) {
				int tx = getWidth()/2 + (int)(t.radius * Math.cos(t.angle) * zoom);
				int ty = getHeight()/2 + (int)(t.radius * Math.sin(t.angle) * zoom);
				double dist = Math.hypot(mx - tx, my - ty);
				if(dist < 20 && t.hp > 0) {
					t.hp -= 30;
					if(t.hp < 0) t.hp = 0;
					t.hitFlashFrame = 5;
					explosions.add(new Explosion() {{ this.x = tx; this.y = ty; }});
					toRemove.add(m);
					break;
				}
			}
			if(m.distance > 400) {toRemove.add(m); }
		}
		missiles.removeAll(toRemove);
		
		//폭발 애니메이션 처리
		ArrayList<Explosion> toRemoveExp = new ArrayList<>();
		for(Explosion ex : explosions) {
			ex.update();
			if(ex.isDone()) toRemoveExp.add(ex);
		}
		explosions.removeAll(toRemoveExp);
		
		//적 감지 및 상태 업데이트
		for(Target t : targets) {
			double angleDiff = Math.abs(scanAngle % (2 * Math.PI) - t.angle);
			angleDiff = Math.min(angleDiff, 2 * Math.PI - angleDiff);  //각도 차이를 0~ㅠ 사이 최소값으로 보정
			
			if(angleDiff < 0.1) {
				t.detectedFrame = 30;  //감지되면 30프레임(약 1초) 유지
				t.hp -= 10;
				if(t.hp < 0) { t.hp = 0;}
			}
			
			//깜빡임 제어 로직
			if(t.detectedFrame > 0) {
				t.detected = true;
				if(t.hp > 0) { t.hp--; }
				t.blinkCounter++;
				if(t.blinkCounter % 6 == 0) {
					t.blinkOn = !t.blinkOn;
					targetFound = !targetFound;
				}
				if(!t.soundPlayed) {
					playSound("/scan_detected.wav");  //wav 파일은 src 기준 경로
					t.soundPlayed = true;  //다음부턴 다시 안 울림
				}
				t.detectedFrame--;
			}
			else {
				if(t.detected) {
					t.detected = false;
					t.blinkOn = true;
					t.blinkCounter = 0;
					t.soundPlayed = false;
					targetFound = false;
				}
			}
			
			if(t.hitFlashFrame > 0) {
				t.hitFlashFrame--;  //1프레임 줄임
			}
			t.lifetime++;  //프레임마다 1 증가
			t.angle += t.angleSpeed;  //프레임마다 위치 회전
		}
		
		//보스 감지 시 효과
		for(Target t : targets) {
			if(t.isBoss && t.detected) { bossWarningFrame = 60; }
		}
		if(bossWarningFrame > 0) bossWarningFrame--;
		
		//붉은 펄스 애니메이션
		if(pulseGrowing) {
			pulseRadius += 1.5;
			if(pulseRadius >= 80) pulseGrowing = false;
		}
		else {
			pulseRadius -= 1.5;
			if(pulseRadius <= 20) pulseGrowing = true;
		}
		
		//점수 처리: 한 번만 반영
		for(Target t : targets) {
			if(t.hp <= 0 && !t.scored) {
				score += t.isBoss ? 300 : 100;
				t.scored = true;
			}
		}
		//오래된 적 제거
		targets.removeIf(t -> t.hp <= 0 || t.lifetime > 1000);  //약 30초
		
		//적의 공격 판정
		attackTimer++;
		if(attackTimer >= 60) {
			attackTimer = 0;
			for(Target t : targets) {
				if(t.hp > 0 && t.detected) {
					playerHP -= t.isBoss ? 15 : 5;
					if(playerHP < 0) playerHP = 0;
				}
			}
		}
		
		//적 총알 생성
		for(Target t : targets) {
			if(Math.random() < 0.01) {
				EnemyBullet b = new EnemyBullet();
				int tX = getWidth() / 2 + (int)(t.radius * Math.cos(t.angle) * zoom);
				int tY = getHeight() / 2 + (int)(t.radius * Math.sin(t.angle) * zoom);
				b.x = tX; b.y = tY;
				
				//방향 벡터 계산
				double dx = getWidth() / 2 - b.x;
				double dy = getHeight() / 2 - b.y;
				double dist = Math.sqrt(dx * dx + dy * dy);
				b.vx = (dx / dist) * 5;
				b.vy = (dy / dist) * 5;
				
				enemyBullets.add(b);
			}
		}
		
		//적 총알 업데이트 및 충돌 처리
		ArrayList<EnemyBullet> toRemoveBullets = new ArrayList<>();
		for(EnemyBullet b : enemyBullets) {
			b.update();
			//플레이어 중앙 기준 충돌 체크
			double dist = Math.hypot(b.x - getWidth() / 2, b.y - getHeight() / 2);
			if(dist < 15) {
				playerHP -= 5;
				if(playerHP < 0) playerHP = 0;
				toRemoveBullets.add(b);
			}
			if(!b.active) {
				toRemoveBullets.add(b);
			}
		}
		enemyBullets.removeAll(toRemoveBullets);
		
		//플레이어 사망 처리
		if(playerHP <= 0 && !gameOver) {
			gameOver = true;
			timer.stop();  //타이머 중단
		}
		
		//라운드 전환 조건: 적이 모두 제거된 경우
		if(!roundInProgress && targets.isEmpty()) {
			currentround++;
			spawnRoundEnemies(currentround);
			roundInProgress = true;
			showRoundText = true;
			roundTextTimer = 60;  //2초 정도
		}
		if(roundInProgress && targets.isEmpty()) {
			roundInProgress = false;
		}
		
		//라운드 텍스트 타이머 감소
		if(roundTextTimer > 0) {
			roundTextTimer--;
		}
		else {
			showRoundText = false;
		}
		
		frameCounter++;
		if(frameCounter >= 150) {  //약 5초마다
			addRandomTarget();
			frameCounter = 0;			
		}
		
		timeCounter++;
		if(timeCounter >= 30) {  //약 1초 지났을 때
			timeElapsed++;
			timeCounter = 0;
		}
			
		repaint();
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		drawWeb(g);			//거미줄
		drawPieSlices(g);	//파이 분할
		drawSpiral(g);		//나선
		drawHUDScan(g);		//스캔 스타일
	}
	
	private void drawSpiral(Graphics g) {
		Graphics2D g2 = (Graphics2D)g;  //더 정밀한 2D 그래픽 객체로 캐스팅
		//중심 좌표
		int centerX = getWidth() / 2;
		int centerY = getHeight() / 2;
		//나선의 매개변수
		double a = 0;
		double b = 5;  //숫자를 바꾸면 간격이 넓거나 좁아짐
		g2.setStroke(new BasicStroke(2));
		g2.setColor(Color.RED);
		//이전 좌표
		double prevX = centerX;
		double prevY = centerY;
		//각도 범위(0 ~ 8파이)
		for(double theta = 0; theta < 8 * Math.PI; theta += 0.01) {
			double r = a + b * theta;
			double x = centerX + r * Math.cos(theta) * zoom;  //zoom 적용
			double y = centerY + r * Math.sin(theta) * zoom;

			g2.draw(new Line2D.Double(prevX, prevY, x, y));
			prevX = x;
			prevY = y;
		}
	}
	
	//거미줄 그리기(Web Grid)
	private void drawWeb(Graphics g) {
		Graphics2D g2 = (Graphics2D)g;
		g2.setStroke(new BasicStroke(1));
		g2.setColor(Color.LIGHT_GRAY);
		int centerX = getWidth() / 2;
		int centerY = getHeight() / 2;
		//방사형 선(ray lines)
		int numRays = 20;
		for(int i=0; i < numRays; i++) {
			double angle = 2 * Math.PI * i / numRays;
			int x = centerX + (int)(350 * Math.cos(angle));
			int y = centerY + (int)(350 * Math.sin(angle));
			g2.drawLine(centerX, centerY, x, y);
		}
		//원형 고리(concentric circles)
		//numCircles = 6;
		for(int r = 50; r <= 300; r += 50) {
			int scaledR = (int)(r * zoom);  //zoom 적용
			g2.drawOval(centerX - scaledR, centerY - scaledR, 2 * scaledR, 2 * scaledR);
		}
	}
	
	//원형 분할(수학적 파이 조각 시각화)
	private void drawPieSlices(Graphics g) {
		Graphics2D g2 = (Graphics2D)g;
		g2.setColor(Color.BLUE);
		int centerX = getWidth() / 2;
		int centerY = getHeight() / 2;
		int radius = (int)(300 * zoom);  //zoom 적용
		int slices = 24;
		
		for(int i=0; i<slices; i++) {
			double angle = 2 * Math.PI * i / slices;
			int x = centerX + (int)(radius * Math.cos(angle));
			int y = centerY + (int)(radius * Math.sin(angle));
			g2.fillOval(x - 3, y - 3, 6, 6);  //작은 점
		}
	}
	
	//HUD 스캔 효과 그리기
	private void drawHUDScan(Graphics g) {
		Graphics2D g2 = (Graphics2D)g;
		int centerX = getWidth() / 2;
		int centerY = getHeight() / 2;
		
		//1. 스캔 원 + 회전 스캐선
		g2.setColor(new Color(0, 255, 0, 50));
		double pulse = 10 * Math.sin(System.currentTimeMillis() / 300.0);
		int r = (int)((310 + pulse) * zoom);  //zoom 적용!
		g2.fillOval(centerX - r, centerY - r, 2 * r, 2 * r);
		
		g2.setColor(Color.GREEN);
		double angle = scanAngle;
		int x = centerX + (int)(r * Math.cos(angle));
		int y = centerY + (int)(r * Math.sin(angle));
		g2.drawLine(centerX, centerY, x, y);
		
		//SCAN 텍스트 + 끝점 효과
		g2.setFont(new Font("Consolas", Font.BOLD, 18));
		g2.setColor(Color.BLUE);
		g2.drawString("SCAN", x + 10, y);
		g2.setColor(new Color(255, 0, 0, (int)(100 + 50 * Math.sin(System.currentTimeMillis() / 100.0))));
		g2.fillOval(x - 5, y - 5, 10, 10);
		
		//2. 타겟 감지 텍스트
		if(targetFound) {
			g2.setColor(Color.RED);
			g2.setFont(new Font("Consolas", Font.BOLD, 24));
			g2.drawString("TARGET FOUND!", centerX - 100, centerY);
		}
		
		//3. 회전 각도 텍스트
		String angleText = String.format("%.0f", Math.toDegrees(scanAngle) % 360);
		g2.drawString(angleText, x + 10, y - 20);
		
		//4. 점수, 타이머, 라운드
		g2.setColor(Color.BLACK);
		g2.setFont(new Font("Consolas", Font.BOLD, 18));
		g2.drawString("Score: " + score, 20, 30);
		g2.drawString("Time: " + timeElapsed + "s", 20, 55);
		g2.drawString("ROUND: " + currentround, getWidth() - 150, 30);
		
		//5. 라운드 시작 텍스트
		if(showRoundText && roundTextTimer > 0) {
			g2.setColor(new Color(255, 0, 0, 150));
			g2.setFont(new Font("Consolas", Font.BOLD, 36));
			g2.drawString("ROUND " + currentround, centerX - 100, 80);
		}
		
		//6. 타겟 그리기
		for(Target t : targets) {
			int tX = centerX + (int)(t.radius * Math.cos(t.angle) * zoom);
			int tY = centerY + (int)(t.radius * Math.sin(t.angle) * zoom);
			
			//체력 바
			int barWidth = 40;
			int hpWidth = (int)((t.hp / 100.0) * barWidth);
			g2.setColor(Color.GRAY);  //배경 바
			g2.fillRect(tX - barWidth/2, tY + 20, barWidth, 6);
			g2.setColor(Color.GREEN);  //체력 바
			g2.fillRect(tX - barWidth/2, tY + 20, hpWidth, 6);
			
			//정보 텍스트
			g2.setFont(new Font("Consolas", Font.BOLD, 12));
			g2.setColor(Color.ORANGE);
			String info = String.format("[%s] HP: %d D: %.0f A: %.0f", t.name, t.hp, t.radius, Math.toDegrees(t.angle) % 360);
			g2.drawString(info, tX - 30, tY - 15);
			
			//경고 텍스트(보스)
			if(bossWarningFrame > 0 && bossWarningFrame % 20 < 10) {
				g2.setColor(Color.RED);
				g2.setFont(new Font("Consolas", Font.BOLD, 24));
				g2.drawString("!! WARNING: BOSS DETECTED !!", getWidth()/2 -150, 40);
			}
			
			//타겟 색상
			if(!t.detected || t.blinkOn) {
				if (t.hitFlashFrame > 0) {g2.setColor(Color.WHITE);}  //피격 시 하얀색 점멸
				else if(t.hp <= 0) {g2.setColor(Color.DARK_GRAY);}  //죽은 적은 회색
				else if(t.isBoss) {g2.setColor(Color.ORANGE);}  //보스는 주황색
				else if (t.detected){g2.setColor(Color.RED);}  //감지 중이면 빨강
				else {g2.setColor(Color.GRAY);}  //감지 전은 회색
				
				int radius = t.isBoss ? 12 : 8;
				g2.fillOval(tX - radius, tY - radius, radius * 2, radius * 2);
				
				//예측 위치 시각화(점선)
				double futureAngle = t.angle + t.angleSpeed * 30;  //약 1초 뒤
				int futureX = centerX + (int)(t.radius * Math.cos(futureAngle) * zoom);
				int futureY = centerY + (int)(t.radius * Math.sin(futureAngle) * zoom);
				g2.setColor(new Color(0, 200, 255, 180));  //반투명 파란색
				g2.setStroke(new BasicStroke(
						1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
						0, new float[] {5}, 0  // <- {5}는 점선 길이, 두 번째 0은 시작 오프셋
				));
				g2.drawLine(tX, tY, futureX, futureY);
				g2.setStroke(new BasicStroke(1));
				g2.fillOval(futureX - 3, futureY - 3, 6, 6);
				
				//감지된 보스 테두리
				if(t.isBoss && t.detected) {
					g2.setStroke(new BasicStroke(3));
					g2.setColor(Color.YELLOW);
					g2.drawOval(tX - radius - 2, tY - radius - 2, (radius + 2) * 2, (radius + 2) * 2);
				}
			}
			
			//보스 아우라
			if(t.isBoss && t.detected) {
				g2.setColor(new Color(255, 0, 0, 100));  //반투명 붉은 아우라
				g2.fillOval(tX - 30, tY - 30, 60, 60);
			}
		}
		
		//7. 붉은 펄스 원
		g2.setColor(new Color(255, 0, 0, 80));
		int pulseR = (int)(pulseRadius);
		g2.fillOval(centerX - pulseR, centerY - pulseR, 2 * pulseR, 2 * pulseR);
		
		//8. 미사일
		for(Missile m : missiles) {
			int mx = centerX + (int)(m.distance * Math.cos(m.angle));
			int my = centerY + (int)(m.distance * Math.sin(m.angle));
			g2.setColor(Color.YELLOW);
			g2.fillOval(mx - 3, my - 3, 6, 6);
		}
		
		//9. 폭발 효과
		for(Explosion ex : explosions) {
			g2.setColor(new Color(255, 200, 0, ex.alpha));  //주황색 투명
			g2.fillOval(ex.x - ex.radius, ex.y - ex.radius, ex.radius * 2, ex.radius * 2);
		}
		
		//10. 플레이어 체력
		g2.setColor(Color.BLACK);
		g2.setFont(new Font("Consolas", Font.BOLD, 16));
		g2.drawString("Player HP", 20, getHeight() - 40);
		g2.setColor(Color.GRAY);  //배경 바
		g2.fillRect(20, getHeight() - 30, 100, 10);
		g2.setColor(Color.RED);  //체력 바
		g2.fillRect(20, getHeight() - 30, playerHP, 10);
		
		if(playerHP <= 0) {  //체력이 0이면 GAME OVER 메시지
			g2.setColor(Color.RED);
			g2.setFont(new Font("Consolas", Font.BOLD, 32));
			g2.drawString("GAME OVER", getWidth()/2 - 100, getHeight()/2 - 20);
		}
		
		//11. 적의 반격 총알
		for(EnemyBullet b : enemyBullets) {
			g2.setColor(Color.BLUE);
			g2.fillOval((int)b.x - b.radius, (int)b.y - b.radius, b.radius * 2, b.radius * 2);
		}
	}
	
	//효과음 재생 메서드
	private void playSound(String filename) {
		try {
			AudioInputStream audioIn = AudioSystem.getAudioInputStream(getClass().getResource(filename));
			Clip clip = AudioSystem.getClip();
			clip.open(audioIn);
			clip.start();
		} catch (Exception e) {e.printStackTrace();}
	}
	
	//마우스 클릭 시 적을 타격
	private void handleClick(int mouseX, int mouseY) {
		int centerX = getWidth() / 2;
		int centerY = getHeight() / 2;
		for(Target t : targets) {
			int tX = centerX + (int)(t.radius * Math.cos(t.angle) * zoom);
			int tY = centerY + (int)(t.radius * Math.sin(t.angle) * zoom);
			//거리 계산
			int dx = tX - mouseX;
			int dy = tY - mouseY;
			double distance = Math.sqrt(dx * dx + dy * dy);
			
			if(distance <= 16 && t.hp > 0) {  //클릭 범위 내일 때만 타격
				t.hp -= 20;
				if(t.hp < 0) t.hp = 0;
				//피격 시 하얀 깜빡임 효과 시작
				t.hitFlashFrame = 5;  //5프레임 동안 반짝임
			}
		}
	}
	
	//미사일 발사
	private void fireMissile() {
		if(gameOver) return;  //게임 오버 상태면 발사 금지
		
		Missile m = new Missile();
		m.angle = scanAngle;  //현재 스캔선 방향
		m.distance = 0;
		missiles.add(m);
	}
	
	//랜덤 위치에 적 생성(주기적 호출)
	private void addRandomTarget() {
		Target t = new Target();
		t.angle = Math.random() * 2 * Math.PI;
		t.radius = 100 + Math.random() * 180;
		t.name = "Enemy " + (char)('A' + (int)(Math.random() * 26));  //랜덤 A~Z
		t.hp = 100;
		t.lifetime = 0;
		t.angleSpeed = 0.005 + Math.random() * 0.01;  //약간씩 다른 속도
		
		if(Math.random() < 0.2) {  //20% 확률로 보스
			t.isBoss = true;
			t.hp = 300;
			t.name = "BOSS " + (char)('A' + (int)(Math.random() * 26));
		}
		targets.add(t);
	}
	
	//라운드 시작 시 적 생성
	private void spawnRoundEnemies(int round) {
		for(int i=0; i<round; i++) {
			Target t = new Target();
			t.angle = Math.random() * 2 * Math.PI;
			t.radius = 100 + Math.random() * 180;
			t.name = "Enemy " + (char)('A' + (int)(Math.random() * 26));
			t.hp = 100 + round * 10;
			t.angleSpeed = 0.01 + Math.random() * 0.01;
			t.lifetime = 0;
			
			if (Math.random() < 0.15 + round * 0.02) {  //라운드가 올라갈수록 보스 등장 확률 증가
				t.isBoss = true;
				t.name = "BOSS " + (char)('A' + (int)(Math.random() * 26));
				t.hp = 300 + round * 10;
			}
			targets.add(t);
		}
		showRoundBanner = true;
		roundBannerTimer = 60;
	}
	
	//일시정지
	public void pause() {
		timer.stop();
	}
	
	//재개
	public void resume() {
		timer.start();
	}
	
	//리셋(변수 초기화)
	public void reset() {
		playerHP = 100;
		score = 0;
		timeElapsed = 0;
		attackTimer = 0;
		timeCounter = 0;
		frameCounter = 0;
		targetFound = false;
		currentround = 1;
		roundInProgress = false;
		roundTextTimer = 60;
		
		missiles.clear();
		explosions.clear();
		enemyBullets.clear();
		targets.clear();
		
		spawnRoundEnemies(currentround);
		gameOver = false;
		timer.start();
	}
	
	//메인 함수 + GUI
	public static void main(String[] args) {
		JFrame frame = new JFrame("HUD Multi Target Demo");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(800, 850);
		
		ArchimedeanSpiralEx hudPanel = new ArchimedeanSpiralEx();
		frame.setLayout(new java.awt.BorderLayout());
		frame.add(hudPanel, java.awt.BorderLayout.CENTER);
		
		//버튼 GUI
		javax.swing.JPanel buttonPanel = new javax.swing.JPanel();
		javax.swing.JButton startBtn = new javax.swing.JButton("Start");
		javax.swing.JButton pauseBtn = new javax.swing.JButton("Pause");
		javax.swing.JButton resetBtn = new javax.swing.JButton("Reset");
		
		buttonPanel.add(startBtn);
		buttonPanel.add(pauseBtn);
		buttonPanel.add(resetBtn);
		frame.add(buttonPanel, java.awt.BorderLayout.SOUTH);
		
		//버튼 이벤트
		startBtn.addActionListener(e -> {
			hudPanel.resume();
			hudPanel.requestFocusInWindow();
		});
		pauseBtn.addActionListener(e -> {
			hudPanel.pause();
			hudPanel.requestFocusInWindow();
		});
		resetBtn.addActionListener(e -> {
			hudPanel.reset();
			hudPanel.requestFocusInWindow();
		});
		
		frame.setVisible(true);
	}
	
	//타겟 클래스
	class Target{
		String name;
		double angle; 				//방향(라디안)
		double radius; 				//거리(픽셀)
		boolean detected = false;  	//감지 중 여부
		int detectedFrame = 0;  	//감지 지속 시간
		boolean blinkOn = true;
		int blinkCounter = 0;
		boolean soundPlayed = false;
		int hp = 100;  				//최대 체력
		int hitFlashFrame = 0;  	//피격 깜빡임 지속 프레임 수
		int lifetime = 0;  			//생성된 뒤 몇 프레임이 지났는지 추적
		double angleSpeed = 0;  	//타겟 회전 속도
		boolean scored = false;  	//이미 점수 반영된 적인지 체크
		boolean isBoss = false;
	}
	
	//미사일 클래스
	class Missile{
		double angle;
		double distance;
		double speed = 10;
		boolean active = true;
	}
	
	//폭발 이벤트 클래스
	class Explosion{
		int x, y;			//위치
		int radius = 0;		//현재 반지름
		int maxRadius = 30;	//최대 크기
		int alpha = 255;	//투명도
		
		void update() {
			radius += 2;
			alpha -= 20;
			if(alpha < 0) alpha = 0;
		}
		boolean isDone() {
			return radius >= maxRadius || alpha == 0;
		}
	}
	
	//적의 반격 총알 클래스
	class EnemyBullet{
		double x, y;
		double vx, vy;
		int radius = 5;
		boolean active = true;
		
		public void update() {
			x += vx;
			y += vy;
			if(x < 0 || x > getWidth() || y < 0 || y > getHeight()) {
				active = false;
			}
		}
	}
}
