# VisionScan-HUD-Simulator
Java로 구현한 HUD 스타일의 게임 시뮬레이터 - 타겟 탐지, 보스 경고, 라운드 시스템, 미사일 발사, 반격 총알, 스캔 애니메이션 구현

---

## 주요 기능

- **스캔 HUD UI**  
  회전하는 스캔선과 확대/축소 애니메이션으로 적을 감지합니다.

- **다중 타겟 감지**  
  감지된 적은 붉게 표시되고, 감지 시 효과음이 재생됩니다.

- **보스 감지 및 경고 시스템**  
  보스를 감지하면 경고 문구와 붉은 펄스, 아우라, 테두리 등이 나타납니다.

- **적의 반격 시스템**  
  일정 확률로 적이 플레이어에게 총알을 발사하며, 피격 시 체력이 감소합니다.

- **라운드 진행 시스템**  
  적이 모두 제거되면 자동으로 다음 라운드가 시작되며, 난이도가 점점 상승합니다.

- **플레이어 체력, 점수, 시간 표시**  
  HUD 스타일로 화면에 실시간 출력됩니다.

- **미사일 시스템**  
  스페이스바로 발사. 감지된 적 또는 클릭한 위치에 미사일이 충돌하면 폭발 이펙트 발생.

---

## 실행 방법

1. Java IDE (예: Eclipse, IntelliJ)에서 새 Java 프로젝트를 생성합니다.
2. `ArchimedeanSpiralEx.java` 파일과 `scan_detected.wav` 파일을 같은 `exercise01` 패키지 내부에 추가합니다.
3. `scan_detected.wav` 파일은 소스 코드 기준 `getClass().getResource("/scan_detected.wav")`로 불러오기 때문에 **`src` 폴더 바로 밑에 위치해야 합니다.**
4. 실행 시, 마우스 클릭 또는 스페이스바로 미사일을 발사하고 적을 제거하세요.

---

## 실행 예시 화면

> 적 감지, 보스 경고, 미사일, 라운드 정보가 표시되는 HUD 스타일 인터페이스
![enemy_detection](https://github.com/user-attachments/assets/a8119ee2-15c5-4bef-b340-a50097a98aa6)
> ![BossWarning_scan](https://github.com/user-attachments/assets/3c761c2f-fb65-4afb-9d8f-2f8f6d6108d7)
> ![exploreMissile_scan](https://github.com/user-attachments/assets/3913169d-0b04-448f-8c30-0d5d362ee564)
![round_change](https://github.com/user-attachments/assets/1ac44cf2-7a00-47fd-8a0d-7d52a32fdb15)

---

## 시연 영상 (Demo)

[**시연 영상 보러가기**]([https://github.com/user-attachments/assets/51db5d08-9f10-4b9e-8a0d-9c7dd8de4781])

---

## 프로젝트 정보

- **프로젝트명:** VisionScan-HUD-Simulator
- **제작자:** Namul (2025)
- **언어:** Java (Swing, AWT 기반)
- **기타:** 효과음 `wav` 사용 / 타이머 기반 애니메이션 / 키보드+마우스 인터랙션

---

## 디렉토리 구조 예시

VisionScan-HUD-Simulator/
├── ArchimedeanSpiralEx.java
├── scan_detected.wav
└── README.md

---

> **Note:** 본 프로젝트는 Java 8 이상에서 실행 가능합니다.  
> 사운드 파일 누락 시, 감지 효과음이 재생되지 않을 수 있습니다.
