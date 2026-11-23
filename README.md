# Downloads 자동 정리기 (Java Personal Project)

목차 : [문제 정의](#문제-정의) · [요구사항](#requirements) · [실행 방법](#실행-방법) · [설계](#structure) · [구현 단계](#구현-단계) · [테스트 기록](#테스트-기록) · [변경 이력](#변경-이력)

## 문제 정의
### 문제
* 학년이 올라갈수록 과목과 자료가 늘어 파일 관리가 점점 힘들어짐
* 학원 조교 일까지 겹쳐 하루에 내려받는 파일만 수십 개. 전부 `Downloads` 폴더 한곳에 섞여 쌓임
* 필요한 파일을 못 찾아 같은 파일을 다시 받거나 찾는 데 시간을 버림. 자주 쓰는 파일이 정리 안 된 걸 볼 때마다 스트레스

### 사용자
* **누가** : 수업 자료와 조교 업무 파일을 하루 수십 개씩 내려받는 대학생(본인). Windows PC, 브라우저 다운로드는 전부 `Downloads` 폴더로 감
* **무엇이 불편한가** : 과목별로 직접 옮기지 않으면 Downloads에 섞여 쌓이고, 찾지 못해 같은 파일을 또 받음
* **파일 이름의 특징** : 과목마다 이름 패턴이 정해져 있음 (`김우성_ch7. datalink.pptx`, `Lab03_과제.zip`, `5주차_고웹프_수업자료.zip` 등) → 이름만 보고 과목을 판단할 수 있음

### 해결 방법
`Downloads` 폴더를 감시하다가, `config.json`에 적은 **파일 이름 규칙**에 맞는 파일을 과목 폴더로 옮기고 이름도 규칙대로 바꾼다.  
실수로 엉뚱한 파일을 옮기지 않도록 **옮기기 전에 미리보기 → 승인(y)해야 이동**, 잘못 옮겼으면 **되돌리기(u)**.

주제 선정 기준 : ① 과한 외부 모듈 없이(Gson 하나) ② 수업에서 배운 다양한 문법을 쓸 수 있고 ③ 난이도가 너무 높지 않으며 ④ 단계별로 살을 붙여 가는 구현 과정이 직관적일 것

### 사용 시나리오
1. 아침에 프로그램을 켠다 → 어제 정리 못 한 파일이 **어디로 갈지 미리보기**로 뜬다 → `y` 입력 → 과목 폴더로 이동하면서 이름도 바뀜  
   `김우성_ch7. datalink.pptx` → `데이터통신/ch_7.pptx`, `Lab03_과제.zip` → `WebServerProgram/lab03.zip`, `5주차_고웹프_수업자료.zip` → `고웹프/react_5.zip`
2. 켜 둔 채로 수업 자료를 내려받는다 → 다운로드가 **끝난 뒤에** 미리보기가 뜬다 (`.crdownload` 상태일 때는 무시) → `y`
3. 잘못 옮겨졌다 → `u` 한 번에 방금 승인한 묶음이 원래 자리로 돌아간다
4. 새 과목이 생겼거나 파일 이름 패턴이 바뀌었다 → 코드는 그대로 두고 `config.json`의 규칙만 수정
5. 규칙에 없는 파일(영수증 사진, 설치 파일 등)은 건드리지 않고 그대로 둔다

## Requirements
### Function Requirements
* 폴더 감시 : 지정한 폴더에 새 파일이 생기면 감지
* 규칙 매칭 : 확장자, 파일명 키워드(정규식)로 어떤 규칙에 해당하는지 판단
* 이름 변경 : 규칙에 정의된 패턴대로 파일명 변경
* 파일 이동 : 규칙에 지정된 대상 폴더로 이동, 폴더가 없으면 생성
* 충돌 처리 : 같은 이름 파일이 있으면 (1), (2) 붙이기
* 기존 파일 일괄 정리 : 실행 시 폴더에 이미 있는 파일도 한 번에 정리
* 미리보기와 승인 : 옮기기 전에 '무엇이 어디로 갈지' 출력하고, 승인(y)해야 실제로 이동
* 로그와 되돌리기 : 처리 내역을 기록하고. 마지막 작업을 원상복구
### Non-Functional Requirements
* 규칙은 코드 수정 없이 설정 파일(Json)로 추가/변경 가능
* 다운로드 중인 임시 파일은 무시
* 오류가 나도 프로그램 전체가 죽지 않고 해당 파일만 건너뛰기

### 구현 범위
| 포함 | 제외 (이번 프로젝트에서 하지 않음) |
|---|---|
| 감시 폴더 1개(`watchDir`)의 **직속 파일** | 하위 폴더 안의 파일, 여러 폴더 동시 감시 |
| **파일 이름**(확장자·와일드카드·정규식)으로 과목 판단 | 파일 내용을 열어 보고 분류 |
| 콘솔에서 미리보기·승인·되돌리기 | GUI·트레이 아이콘 (7단계 선택 항목, 미구현) |
| Windows 11 + JDK 21에서 확인 | 다른 OS에서의 동작 보장 |

### 우선순위와 완료 기준
우선순위 : **필수** = 1차 검증(2단계)까지 갖춰야 "정리기"로 쓸 수 있는 것 / **핵심** = 3~6단계에서 편의·안전을 더하는 것 / **선택** = 7단계  
검증 열의 `T09-…` `T10-…` `T11-…`는 아래 [테스트 기록](#테스트-기록)의 주차별 테스트 항목

| ID | 요구사항 | 우선순위 | 완료 기준 (이걸 확인하면 완료) | 검증 | 결과 |
|---|---|---|---|---|---|
| F1 | 폴더 감시 | 핵심 | 감시 중 새 파일을 넣으면 크기가 멈춘 뒤 **한 번만** 미리보기에 뜬다. 쓰는 중에는 뜨지 않는다 | T11-3, T11-4, T11-8, T11-8R | 통과 ([발견 #2](#발견한-문제와-수정) 수정 후) |
| F2 | 규칙 매칭 | 필수 | config 규칙 4개(WebServerProgram, 데이터통신, 고웹프, Java)에 예시 파일이 기대한 규칙으로 매칭. 조건은 AND, 위 규칙 우선, 대소문자 무시 | T11-1, T11-2, T11-10b | 통과 (규칙 변경 전 [발견 #5](#발견한-문제와-수정)) |
| F3 | 이름 변경 | 핵심 | `Lab03_과제.zip` → `lab03.zip`, `김우성_ch7. datalink.pptx` → `ch_7.pptx`, `5주차_고웹프_수업자료.zip` → `react_5.zip`. `{name}` `{ext}` `{n}` `{yyyy-MM}` 치환, 금지 문자는 `_` | T11-1, T10-4, T10-4R | 통과 ([발견 #1](#발견한-문제와-수정) 수정 후) |
| F4 | 파일 이동 | 필수 | 대상 폴더가 없으면 만들고 이동, 원래 자리에는 남지 않음 | T10-1 | 통과 |
| F5 | 충돌 처리 | 필수 | 같은 이름이 있으면 `(1)`, `(2)`. 확장자 없는 파일도 | T10-2 | 통과 |
| F6 | 기존 파일 일괄 정리 | 필수 | 실행하면 이미 있던 파일 중 규칙에 맞는 것만 목록에 뜨고, `y` 후 전부 이동 | T10-6 | 통과 |
| F7 | 미리보기와 승인 | 핵심 | 미리보기만으로는 파일·기록이 안 바뀜. 미리보기 경로 = 실제 이동 경로. `n`이면 그대로 | T11-5, T10-2 | 통과 |
| F8 | 로그와 되돌리기 | 핵심 | `u` 한 번에 마지막 승인 묶음이 원위치, 다시 누르면 그 이전. 원래 자리에 같은 이름이 있으면 덮어쓰지 않음 | T11-6, T11-7 | 통과 |
| N1 | 규칙은 JSON으로 | 핵심 | 코드 수정 없이 규칙 추가. 필수 값이 빠지면 어떤 값/규칙인지 알려주고 종료 | T09-2, T10-5 | 통과 |
| N2 | 임시 파일 무시 | 필수 | `.crdownload` `.part` `.tmp` `.download`, `~$` 잠금 파일, 숨김 파일은 목록·감시에서 제외 | T10-3, T11-4 | 통과 |
| N3 | 오류는 그 파일만 | 필수 | 한 파일에서 오류(잘못된 rename 등)가 나도 나머지 파일은 처리 | T10-4 | 통과 |

## 실행 방법
### 폴더 배치
이 저장소는 `Java_Project` **패키지 폴더 자체**라서, 아래처럼 프로젝트 루트의 `src/Java_Project`에 받아야 함
```
javaWorkSpace/                 ← 프로젝트 루트 (명령은 모두 여기서 실행)
├─ lib/gson-2.11.0.jar         ← 직접 받아서 넣기 (저장소에 없음)
├─ out/                        ← 컴파일 결과 (자동 생성)
└─ src/Java_Project/           ← 이 저장소
   ├─ config.json              ← 규칙 설정 (기본 설정 파일)
   ├─ history.log              ← 이동 기록 (자동 생성, 되돌리기에 사용)
   └─ app/ config/ core/ history/ util/
```

### 준비 (처음 한 번)
1. **JDK 17 이상** 설치 (switch 식, `Stream.toList()` 사용. JDK 21에서 확인함). 터미널에서 `java -version`으로 확인
2. 프로젝트 루트(예: `javaWorkSpace`)에서 저장소 받기
   ```
   git clone https://github.com/tangerlim/Java_Project_FileMover.git src/Java_Project
   ```
3. **Gson 받기** : https://repo1.maven.org/maven2/com/google/code/gson/gson/2.11.0/gson-2.11.0.jar 를 `lib/` 폴더에 저장  
   IntelliJ를 쓰면 `File > Project Structure > Libraries > + > Java`로 이 jar 추가 (또는 `+ > From Maven…` → `com.google.code.gson:gson:2.11.0`)
4. **config.json 경로 바꾸기** : `watchDir`(감시할 폴더), `baseDir`(과목 폴더들을 만들 위치)를 내 PC 경로로. JSON이라 `\`는 `\\`로 적기  
   처음 실행하면 미리보기만 뜨고 `y`를 눌러야 실제로 옮겨지므로, 먼저 `n`을 눌러 어디로 갈지 확인한 뒤 규칙을 다듬으면 안전함. 잘못 옮겼으면 `u`

### 실행
**IntelliJ** : `app/Main` 실행. 작업 디렉터리 = 프로젝트 루트. 다른 config를 쓰려면 `Run > Edit Configurations > Program arguments`에 경로 입력

**VS Code·터미널 (PowerShell)** : 프로젝트 루트에서
```powershell
# 컴파일 (코드를 고칠 때마다 다시)
javac -encoding UTF-8 -cp lib\gson-2.11.0.jar -d out (Get-ChildItem src\Java_Project -Recurse -Filter *.java).FullName

# 실행 (마지막 인자 = 사용할 config, 생략하면 src/Java_Project/config.json)
java "-Dstdout.encoding=UTF-8" -cp "out;lib\gson-2.11.0.jar" Java_Project.app.Main src\Java_Project\config.json
```
**Git Bash** :
```bash
javac -encoding UTF-8 -cp lib/gson-2.11.0.jar -d out $(find src/Java_Project -name "*.java")
java -Dstdout.encoding=UTF-8 -cp "out;lib/gson-2.11.0.jar" Java_Project.app.Main src/Java_Project/config.json
```
`-cp` 값의 **따옴표를 빼면 안 됨** (`;`가 명령 구분자로 해석돼 실행이 안 됨). macOS/Linux는 `;` 대신 `:`

### 사용법
```
1. 기존 파일 미리보기   Downloads에 이미 있는 파일이 어디로 갈지 번호 붙여 출력
                       → y: 이동 / n: 그대로 두기
2. 실시간 감시 시작     q 입력할 때까지 계속
   새 파일 감지 시      미리보기 출력 → y: 이동 / n: 건너뛰기
   u                   마지막 작업 되돌리기 (누를 때마다 하나씩 이전으로)
   q                   종료
```
실행 예 (실제 Downloads, `n` 입력):
```
규칙 4개 로드: [Rule{WebServerProgram -> WebServerProgram / lab{1}.{ext}}, Rule{데이터통신 -> 데이터통신 / ch_{1}.{ext}}, ...]

[기존 파일 미리보기] C:\Users\likgr\Downloads
   1. 김우성_ch01_컴퓨터 시스템의 소개.pptx
      -> C:\Users\likgr\Desktop\--\데이터통신\ch_01.pptx
   ...
   5. 김우성_ch8. wireless.pptx
      -> C:\Users\likgr\Desktop\--\데이터통신\ch_8.pptx
위 5개 파일을 이동할까요? (y: 이동 / n: 건너뛰기) > n
기존 파일은 그대로 둡니다.
```

### 문제 해결
| 증상 | 원인 | 해결 |
|---|---|---|
| `설정 파일을 읽을 수 없습니다 (…)` | config 경로가 틀렸거나 파일이 없음 | 메시지에 나온 절대 경로에 파일이 있는지 확인. 명령은 프로젝트 루트에서 실행 |
| 한글이 `����`로 깨짐 | 터미널 인코딩 | 실행 명령에 `-Dstdout.encoding=UTF-8` 붙이기 |
| `NoClassDefFoundError: com/google/gson/…` | 실행할 때 Gson jar가 `-cp`에 안 들어감 (따옴표 누락으로 `;` 뒤가 잘림 등) | `-cp "out;lib\gson-2.11.0.jar"`처럼 따옴표로 감싸기, `lib/`에 jar가 있는지 확인 |
| `Could not find or load main class` | 컴파일을 안 했거나 프로젝트 루트가 아닌 곳에서 실행 | 프로젝트 루트에서 컴파일부터 다시 |
| Downloads에 파일이 있는데 `규칙에 맞는 기존 파일이 없습니다.` | 파일 이름이 규칙과 안 맞음 | 아래 규칙 표와 파일 이름을 비교해 `config.json` 수정 ([발견 #5](#발견한-문제와-수정)) |
| 잘못 옮겨짐 | — | 감시 중 `u` 입력. 프로그램을 껐다 켜도 `history.log`가 남아 있으면 되돌릴 수 있음 |

### 규칙 작성 (config.json)
```json
{
  "watchDir": "C:\\Users\\likgr\\Downloads",
  "baseDir": "C:\\Users\\likgr\\Desktop\\--",
  "rules": [
    {
      "name": "WebServerProgram",
      "match": { "namePattern": "^(?=.*lab)\\D*(\\d+)(?=.*\\.)" },
      "target": "WebServerProgram",
      "rename": "lab{1}.{ext}"
    },
    {
      "name": "데이터통신",
      "match": { "namePattern": "(?<![a-z])ch(\\d+)" },
      "target": "데이터통신",
      "rename": "ch_{1}.{ext}"
    },
    {
      "name": "고웹프",
      "match": { "namePattern": "(\\d+)\\s*주차.*수업자료" },
      "target": "고웹프",
      "rename": "react_{1}.{ext}"
    },
    { "name": "Java", "match": { "glob": "*주차*강의자료*" }, "target": "Java" }
  ]
}
```
| 규칙 | 매칭 조건 | 예시 |
|---|---|---|
| WebServerProgram | 이름에 `lab`(대소문자 무관) + 숫자. 이름의 첫 숫자를 사용 | `Lab03_과제.zip` → `lab03.zip`, `week5_LAB.pdf` → `lab5.pdf` |
| 데이터통신 | 이름 어디든 `ch` + 숫자 (앞이 영문자면 제외: `search2`는 안 잡힘). 운영체제 자료도 교수님이 같아서 여기로 통일 | `ch2application_261002.pdf` → `ch_2.pdf`, `김우성_ch7. datalink.pptx` → `ch_7.pptx` |
| 고웹프 | `숫자주차 … 수업자료` | `5주차_고웹프_수업자료.zip` → `react_5.zip` |
| Java | `*주차*강의자료*` | 이름 그대로 이동 |

* WebServerProgram 규칙은 확장자 앞 이름 부분의 숫자만 봄 (`lab.mp4`의 `4`는 무시). `lab`이 있어도 숫자가 없으면 매칭되지 않음
* 규칙은 위에서부터 검사하므로, `lab`이 들어간 파일은 다른 규칙 조건을 만족해도 WebServerProgram으로 감
* 같은 장 번호·같은 확장자 파일이 또 오면 덮어쓰지 않고 `ch_8 (1).pdf`처럼 번호가 붙음

| 항목 | 설명 |
|---|---|
| `watchDir` | 감시할 폴더 |
| `baseDir` | `target`이 상대경로일 때 기준 폴더 (생략하면 `watchDir`) |
| `rules` | 위에서부터 검사, 처음 맞는 규칙 하나만 적용 |
| `match.extensions` | 확장자 목록 (점 없이, 대소문자 무시) |
| `match.glob` | 파일명 와일드카드 (`*`, `?`, `[0-9]`) |
| `match.namePattern` | 정규식 (대소문자 무시, 파일명 일부만 맞아도 됨). JSON이라 `\`는 `\\`로 |
| `target` | 옮길 폴더 |
| `rename` | 새 이름 (선택). `{name}` 원래 이름, `{ext}` 확장자, `{1}` 정규식 그룹, `{yyyy-MM}` 날짜 |
| `stableSeconds` | (선택, 기본 2) 파일 크기가 이 시간 동안 안 변하면 다운로드 완료로 판단 |
| `historyFile` | (선택) 처리 기록 파일 위치. 기본은 config.json 옆 `history.log` |

`match` 안의 조건은 적힌 것끼리 모두 만족해야 매칭 (AND)

## Structure

| 경로 | 클래스/모듈 | 설명 |
|---|---|---|
| `app/` | `Main` | 실행 진입점. 기존 파일 미리보기·승인 → 실시간 감시 → 명령(y/n/u/q) 입력 |
| `config/` | `ConfigLoader` | config.json을 Gson으로 읽어 객체로 변환, 필수 값 검사 |
|  | `AppConfig` | 설정 파일 전체 (감시 폴더, 기준 폴더, 규칙 목록) |
|  | `Rule`, `MatchCondition` | 규칙 데이터 (조건, 대상 폴더, 새 이름 패턴) |
| `core/` | `FolderWatcher` | `WatchService`로 이벤트 수신, 다운로드 완료 대기, 중복 이벤트 제거. 시작 직후 한 번 폴더를 훑어 기존 파일 질문 중에 생긴 파일도 처리 |
|  | `ApprovalQueue` | 감시로 들어온 새 파일을 승인 대기 목록에 모아 둠 |
|  | `RuleMatcher` | 파일에 맞는 규칙 찾기 (확장자 / glob / 정규식) |
|  | `RuleMatch` | 매칭 결과 (규칙 + 정규식 그룹 값) |
|  | `NameResolver` | `{1}`, `{ext}`, `{yyyy-MM}` 같은 패턴 치환 |
|  | `MovePlan` | 이동 계획 (원래 파일, 대상 폴더, 새 이름) |
|  | `FileMover` | 파일 이동 및 충돌 처리, 미리보기용 최종 경로 계산 |
|  | `FileProcessor` | 위 기능을 묶어 계획 → 미리보기 → 이동 → 기록 |
| `history/` | `HistoryEntry` | 기록 한 줄 (실행ID, 시각, 원래 위치, 새 위치) |
|  | `HistoryLog` | 처리 내역을 history.log에 저장/읽기 |
|  | `UndoManager` | 마지막 작업을 역순으로 되돌리기 |
| `util/` | `TempFileFilter` | 임시 파일 판별 |


### 데이터 흐름
```
config.json ──Gson──▶ AppConfig { watchDir, baseDir, List<Rule> }
                                         │ (RuleMatcher가 glob/정규식을 미리 컴파일)
파일 Path ──▶ RuleMatch { rule, 정규식 그룹 } ──▶ MovePlan { 원래 파일, 대상 폴더, 새 이름 }
         ──▶ Files.move ──▶ HistoryEntry "실행ID \t 시각 \t 원래 위치 \t 새 위치" ──▶ history.log
                                                                              ──▶ UndoManager (역순 원위치)
```


## 구현 단계
1. 하드코딩한 규칙 하나로 폴더 내 기존 파일을 확장자별 이동 | Files.list, Files.move
2. 충돌 처리, 임시 파일 무시, 예외 처리 | 예외 설계, Path 다루기   
-> 여기까지만 해도 작동. 검증후 이후 단계로 살 붙여나가기. **1차 검증**
3. 규칙을 Json으로 분리 | 객체 매핑
4. 정규식 매칭과 이름 패턴 치환 | Pattern, Matcher 그룹
5. 실시간 감시 모드 | WatchService, 스레드
6. 미리보기, 승인, 로그, 되돌리기 | 설계 확장, 파일 기록
7. 트레이 아이콘이나 간단한 GUI, 테스트 | Swing/JavaFX, 테스트  

### 구성요소 설계
**--week 09--**    
0단계. 프로젝트 준비.
프로젝트 생성 
패키지 폴더 만들기: app, config, core, history, util

**--week 10-- 최소 구현 기능**    
1단계. 기존 파일을 일련의 규칙대로 옮기기
Main — 감시 폴더 경로를 하드코딩하고 Files.list로 파일 목록 출력
FileMover — 파일 하나를 대상 폴더로 이동 
Main에서 와일드카드를 사용하여 규칙에 맞는 파일 감지, 대상 폴더로 이동

> **구현** 
> * `Main` : `Files.list()` + `filter(Files::isRegularFile)`로 일반 파일만 목록화. `PathMatcher`(glob) 4개를 for문 밖에서 미리 만들고 if-else로 대상 폴더 결정
> * `FileMover.move()` : `Files.createDirectories()`로 폴더 만들고 `Files.move()`
> * `MovePlan` : "파일 → 폴더" 묶음 클래스 (이후 단계에서 이름까지 담도록 확장)
   
2단계. 안전장치
TempFileFilter — .crdownload, .part, .tmp 판별
FileMover에 충돌 처리 추가 — 같은 이름이면 (1), (2) 붙이기
파일 하나 실패해도 계속 진행되게 try-catch 정리

> **구현** 
> * `FileMover.resolveUniqueTarget()` : 이름과 확장자를 분리하고 `Files.exists()`가 false일 때까지 ` (n)` 번호 증가
> * `TempFileFilter.isTemp()` : `.crdownload/.part/.tmp/.download` 확장자, `~$`(오피스 잠금), `.`(숨김 파일) 판별. 생성자를 `private`으로 막은 static 유틸 클래스
> * 파일마다 try-catch → `IOException`이 나도 메시지만 출력하고 다음 파일 진행

3단계. 규칙을 객체와 JSON으로 분리
MatchCondition, Rule — 데이터 클래스
RuleMatcher — 파일에 맞는 Rule 찾기 (일단 확장자만)
ConfigLoader — config.json 읽어서 List<Rule>로 변환 (Gson)
FileProcessor — "매칭 → 이동" 흐름 묶기
Main의 if문 하드코딩을 걷어내고 FileProcessor 호출로 교체

> **구현** 
> * `AppConfig` / `Rule` / `MatchCondition` : JSON 구조와 필드 이름을 1:1로 맞춘 클래스 → `gson.fromJson()` 한 줄로 객체 변환
> * `ConfigLoader` : UTF-8로 읽기, `JsonParseException`을 `IOException`으로 감싸 Main에서 한 번에 처리, 필수 값(watchDir, rules, match, target) 검사
> * `RuleMatcher` : 확장자 + 기존 glob 조건 둘 다 지원 (기존 규칙을 그대로 옮기기 위해). `PathMatcher`를 `HashMap`에 캐시, 결과는 `Optional<Rule>`
> * `FileProcessor` : "임시 파일 거르기 → 규칙 찾기 → 이동"을 한 메서드로. Main은 설정 읽기와 부품 조립만 담당
> * Gson jar는 `lib/`에 두고 `javaWorkSpace.iml`에 라이브러리로 등록

**--week 11--**     
4단계. 정규식과 이름 바꾸기
RuleMatcher에 namePattern 매칭 추가
NameResolver — {1}, {ext}, {name}, {yyyy-MM} 치환
FileProcessor에 이름 결정 단계 끼워 넣기

> **구현** 
> * `MatchCondition.namePattern`, `Rule.rename` 필드 추가
> * `RuleMatcher` : 생성자에서 `Pattern.compile()` (대소문자 무시) → 정규식 문법 오류를 실행 시작 시점에 발견. `matcher.find()` 후 그룹 값을 리스트로 담아 `RuleMatch`로 반환
> * `NameResolver` : `\{([^}]+)}` 정규식으로 토큰을 찾고 `appendReplacement()`로 치환. switch 식으로 `name`/`ext`/숫자(그룹)/그 외(날짜, `DateTimeFormatter`) 구분. Windows 금지 문자는 `_`로 바꿈
> * `FileMover` : 새 이름을 받는 `move(source, dir, newName)` 오버로딩 추가, `MovePlan`에 새 이름 필드 추가

**확인: lecture_03.pdf → 데이터통신_03주차.pdf로 바뀌어 이동** (당시 config 규칙 기준. 현재 규칙은 [규칙 작성](#규칙-작성-configjson) 참고)

5단계. 실시간 감시
FolderWatcher — WatchService로 새 파일 이벤트 수신
파일 크기가 안정될 때까지 기다렸다가 처리 (ScheduledExecutorService)
중복 이벤트 걸러내기
Main에 모드 선택 추가: 일괄 정리 / 감시

> **구현** 
> * `FolderWatcher` (`Runnable`) : 별도 스레드에서 `watchService.take()`로 이벤트 대기. `ENTRY_CREATE`/`ENTRY_MODIFY` 수신, `OVERFLOW`면 폴더 전체 재검사
> * 다운로드 완료 판단 : `ScheduledExecutorService`로 일정 시간 뒤 크기를 다시 재서 같고, `FileChannel`로 쓰기 열기가 되면(다른 프로그램이 안 잡고 있으면) 처리. 아니면 다시 예약
> * 중복 제거 : 파일별 예약(`ScheduledFuture`)을 `ConcurrentHashMap`에 두고, 같은 파일 이벤트가 또 오면 이전 예약 취소
> * 종료 : `q` 입력 시 `watchService.close()` → 감시 스레드 종료 후 `join()`. Ctrl+C 대비 shutdown hook 등록

**확인: 프로그램 켜둔 상태에서 파일을 넣으면 자동으로 옮겨짐**

6단계. 미리보기, 승인, 로그, 되돌리기
FileProcessor에 미리보기 — 이동 대신 출력만
HistoryLog — "원래 위치 → 새 위치"를 파일에 기록
UndoManager — 마지막 실행분을 역순으로 되돌리기

> **구현** 
> * 미리보기 : `FileProcessor.previewAll()`/`printPlans()`가 번호 붙여 출력. `FileMover.previewTarget()`이 앞 파일이 차지할 이름까지 `Set`으로 기억해 (1), (2)를 정확히 예측
> * 승인 : 기존 파일은 Main에서 y/n 질문. 새 파일은 `ApprovalQueue`에 쌓아 두고 y = 전부 이동, n = 전부 건너뛰기
> * `HistoryEntry` / `HistoryLog` : 이동할 때마다 탭 구분 한 줄을 append (도중에 꺼져도 기록 유지). 감시 스레드에서도 쓰므로 `synchronized`
> * 되돌리기 단위 : 승인 한 번에 옮긴 묶음 = 실행 1번 (실행ID로 구분)
> * `UndoManager` : 마지막 실행분을 역순으로 원위치. 원래 자리에 같은 이름이 있거나 파일이 없어졌으면 건너뛰고, 실패한 항목은 기록에 남겨 다시 시도 가능
> * 감시 중 되돌리기 : 되돌린 파일을 감시가 다시 잡지 않도록 `FolderWatcher.ignoreFor()`로 5초 무시. y/n/u 명령은 `runOnWorker()`로 파일 처리와 같은 스레드에서 차례로 실행 → 동시에 같은 파일을 건드리지 않음
> * 실행 흐름 개편 : 모드 선택 메뉴 대신 "기존 파일 미리보기·승인 → 실시간 감시"를 기본 흐름으로. `FolderWatcher`는 `Consumer<Path>`로 다음 단계를 받아서 `ApprovalQueue`와 분리

**확인: 정리 후 되돌리기 하면 원래대로 돌아옴**

7단계 (선택). 테스트와 UI
주차별로 구현한 기능을 직접 실행해서 테스트
여유 있으면 트레이 아이콘이나 간단한 Swing 창

> **진행** 
> * week 09~11 구현 내용을 주차별 테스트 17개로 직접 실행해서 확인 (15개 통과, 2개 문제 발견 → 둘 다 수정 후 재확인 통과). 결과와 발견한 문제는 [테스트 기록](#테스트-기록)
> * Swing / 트레이 아이콘은 아직 안 함

### 주의
* 다운로드 직후 파일이 잠겨 있음 : 감지하자마자 옮기면 실패할 수 있어서, 파일 크기가 일정 시간 변하지 않을 때까지 잠깐 기다리는 로직 필요   
  → 해결 : 크기 안정 + 쓰기 잠금 확인 후 처리 (5단계). 최대 약 5분까지 기다리고 포기
* 이벤트 중복 : WatchService는 한 파일에 생성/수정 이벤트를 여러 번 보낼 수 있음   
  → 해결 : 파일별 예약을 하나만 유지, 새 이벤트가 오면 이전 예약 취소 (5단계)
* 자기 자신 감시 : 대상 폴더가 감시 폴더 안에 있으면 옮긴 파일을 다시 감지할 수 있으니 하위 폴더는 감시에서 제외 필요   
  → 해결 : WatchService는 등록한 폴더의 직속 파일만 감시, 폴더 이벤트는 무시 (5단계)
* 되돌린 파일 재감지 : 되돌리기로 Downloads에 돌아온 파일을 감시가 새 파일로 보고 다시 물어봄   
  → 해결 : 되돌린 경로는 5초 동안 감시에서 무시 (6단계)
### 피드백
* 교수님 : 구현 과정은 어렵지 않을 것으로 예상, 근데 실험 단계에서 애로사항이 있을 거임   
  → 실험은 별도 테스트 폴더 없이 실제 Downloads에서 진행. 대신 모든 이동은 미리보기 → 승인(y) 후에만 일어나고, 잘못 옮기면 `u`로 되돌릴 수 있게 해서 실제 파일을 잃지 않도록 함
* 이승혁 학우 : 프로그램 실행시 일련의 작업을 할 것인지, 실행 안 해도 자동으로 감지해서 시행할 것인지.   
  → 둘 다 : 실행하면 기존 파일을 미리보기·승인으로 정리하고, 이어서 끄기 전까지 실시간 감시. 모든 이동은 승인 후에만

## 테스트 기록
### 테스트 환경
| 항목 | 값 |
|---|---|
| OS / JDK | Windows 11 / Eclipse Temurin JDK 21.0.13 |
| 라이브러리 | Gson 2.11.0 |
| 폴더 | 별도 테스트 폴더 없이 **실제 환경 그대로** : 감시 `C:\Users\likgr\Downloads` → 과목 폴더 `C:\Users\likgr\Desktop\--\…`, 설정은 실제 `config.json` |
| 감시 | `stableSeconds` 기본값(2초) |

**방법** : 테스트용 파일을 실제 Downloads에 직접 만들어 넣고(또는 내려받고) 프로그램을 실행하면서 미리보기·이동 결과를 확인. 설정 오류를 보는 테스트(T10-4, T10-5, T11-2)는 `config.json`을 잠깐 고쳐서 실행한 뒤 원래대로 되돌림. 옮겨진 테스트 파일은 `u`로 되돌리거나 지움

### 주차별 테스트 결과 : 17개 중 15개 통과, 2개 문제 발견 → 수정 후 재확인 통과 (+ 규칙 변경 후 재확인 2건)
#### week 09 — 0단계 프로젝트 준비
| ID | 할 일 | 기대 결과 | 결과 |
|---|---|---|---|
| T09-1 | [실행 방법](#실행-방법)의 `javac` 명령(또는 IntelliJ Build)으로 컴파일 | 오류 없이 컴파일됨 | ✔ |
| T09-2 | 빈 `Downloads`로 실행 | `규칙 4개 로드: …` 출력 후 `규칙에 맞는 기존 파일이 없습니다.` → 감시 시작 안내 출력 | ✔ |

#### week 10 — 1~3단계 (기존 파일 이동, 안전장치, JSON 규칙)
| ID | 할 일 | 기대 결과 | 결과 |
|---|---|---|---|
| T10-1 | `config.json`의 Java 규칙 `target`을 아직 없는 폴더 이름(예: `Java_새폴더`)으로 잠깐 바꾸고, `Downloads`에 `9주차_강의자료.pdf` → 실행 → `y` | `Desktop\--\Java_새폴더\` 폴더가 생기고 그 안으로 이동, `Downloads`에는 남지 않음 | ✔ |
| T10-2 | `Desktop\--\Java\`에 `9주차_강의자료.pdf`가 이미 있는 상태에서 같은 이름 파일 2개를 차례로 정리. 확장자 없는 `9주차_강의자료`도 한 번 | 미리보기와 실제 이동 모두 `9주차_강의자료 (1).pdf`, 다음은 `(2)`. 확장자 없는 파일은 `9주차_강의자료 (1)` | ✔ |
| T10-3 | `Lab01.pdf.crdownload`, `Lab02.zip.part`, `ch3.pdf.tmp`, `ch4.pdf.download`, `~$9주차_강의자료.pptx`, `.lab05.zip`을 넣고 실행 | 6개 모두 미리보기 목록에 안 뜨고 그대로 남음 | ✔ |
| T10-4 | `config.json` 맨 위에 `{"name":"오류","match":{"glob":"*err*"},"target":"X","rename":"{1}.{ext}"}` 추가 → `err.txt`, `Lab01.pdf` 넣고 실행 → `y` | `err.txt`만 제외되고 `Lab01.pdf`는 `WebServerProgram/lab01.pdf`로 이동. 제외 사유가 알아보기 쉬움 | **✘** `Lab01.pdf`는 정상 이동(나머지 파일 처리는 통과). 하지만 제외 메시지가 `namePattern에 괄호가 -1개` → [발견 #1](#발견한-문제와-수정) |
| T10-5 | `config.json`을 하나씩 망가뜨리고 실행 (확인 후 원래대로): ① `watchDir` 삭제 ② 한 규칙의 `target` 삭제 ③ 쉼표 하나 삭제 | ① `watchDir가 없습니다` ② `규칙 '…'에 match 또는 target이 없습니다` (규칙 이름 표시) ③ `config.json 형식 오류: …`. 셋 다 이동 없이 종료 | ✔ |
| T10-6 | `Lab03_과제.zip`, `ch2application_261002_235129.pdf`, `9주차_강의자료.pdf`, `영수증.png`를 넣고 실행 → `y` | `영수증.png`를 뺀 3개만 미리보기, `y` 후 3개 모두 이동, `영수증.png`는 그대로 | ✔ |

#### week 11 — 4~6단계 (정규식·이름 변경, 실시간 감시, 미리보기·되돌리기)
| ID | 할 일 | 기대 결과 | 결과 |
|---|---|---|---|
| T11-1 | 아래 파일을 넣고 실행 → `y` | `Lab03_과제.zip` → `WebServerProgram/lab03.zip` · `week5_LAB.pdf` → `WebServerProgram/lab5.pdf` · `lab_report.pdf`(숫자 없음) → 목록에 없음 · `ch2application_261002_235129.pdf` → `데이터통신/ch_2.pdf` · `CH10.pptx` → `데이터통신/ch_10.pptx` · `5주차_고웹프_수업자료.zip` → `고웹프/react_5.zip` · `9주차_강의자료.pdf` → `Java/9주차_강의자료.pdf` · `ch3_lab.pdf` → `WebServerProgram/lab3.pdf` (위 규칙 우선) | ✔ |
| T11-2 | `config.json`의 WebServerProgram `namePattern`에서 닫는 괄호 하나 삭제 후 실행 | 파일 처리 전에 `규칙 오류: 규칙 'WebServerProgram'의 namePattern 오류: …` 출력 후 종료 | ✔ |
| T11-3 | 감시 중 ① `Lab07.zip`을 복사해 넣기 ② 수백 MB 파일을 `Lab08.zip`으로 복사 | ① 약 2초 뒤 `[새 파일 미리보기] 승인 대기 1개`가 **한 번만** 뜸 ② 복사가 끝나기 전에는 안 뜨고 끝난 뒤 한 번 | ✔ |
| T11-4 | 감시 중 브라우저로 실제 파일 다운로드 | `.crdownload` 동안은 안 뜨고, 다운로드 완료 후 원래 이름으로 한 번 뜸 | ✔ |
| T11-5 | 기존 파일 미리보기에서 `n` | 파일 위치와 `history.log` 모두 변화 없음 | ✔ |
| T11-6 | 묶음 A(2개) `y` → 묶음 B(1개) `y` → `u` → `u` | 첫 `u`에 B만 원위치, 두 번째 `u`에 A 2개가 한 번에 원위치 (`[되돌리기 완료] 성공 2건, 실패 0건`) | ✔ |
| T11-7 | `y`로 옮긴 뒤 원래 자리에 같은 이름 파일을 만들고 `u` → 이어서 `n` | `되돌리기 실패 : … 원래 위치에 같은 이름 파일이 이미 있습니다` (덮어쓰지 않음). 이후 `n`에 `승인 대기 중인 파일이 없습니다.` (되돌린 파일을 다시 잡지 않음) | ✔ |
| T11-8 | 실행 직후 기존 파일 `y/n` 질문에 답하기 전에 `Lab09.zip` 생성 → `y` → 감시 시작 후 `y` | 새 파일이 승인 대기에 뜸 | **✘** 안 뜸. `승인 대기 중인 파일이 없습니다.` 파일은 Downloads에 남음 → [발견 #2](#발견한-문제와-수정) |
| T11-9 | `q` | 감시 스레드가 멈추고 프로그램 종료 | ✔ |

#### 규칙 변경 후 실제 Downloads로 재확인 (2026-11-23)
실제 `config.json`으로 실행하고 미리보기에서 `n` 입력 (파일은 옮기지 않음). 당시 Downloads에는 `김우성_ch01_컴퓨터 시스템의 소개.pptx`, `김우성_ch02_운영체제의 소개.pdf`, `김우성_ch7. datalink.pptx`, `김우성_ch8. wireless.pdf`, `김우성_ch8. wireless.pptx` 외 규칙과 상관없는 파일들이 있었음

| ID | 데이터통신 규칙 | 기대 결과 | 결과 |
|---|---|---|---|
| T11-10a | `^ch(\d+)` (`ch`로 **시작**하는 이름만) | 위 5개가 데이터통신으로 미리보기 | **✘** `규칙에 맞는 기존 파일이 없습니다.` → [발견 #5](#발견한-문제와-수정) |
| T11-10b | `(?<![a-z])ch(\d+)` (이름 **어디든** `ch숫자`, 앞이 영문자면 제외) | 위 5개가 데이터통신으로 미리보기, 나머지 파일은 목록에 없음 | ✔ `ch_01.pptx`, `ch_02.pdf`, `ch_7.pptx`, `ch_8.pdf`, `ch_8.pptx`로 미리보기. 설치 파일·문서 등은 목록에 없음 |

#### 발견 #1, #2 수정 후 재확인 (2026-11-23)
실제 Downloads에 테스트 파일을 만들어 실행하고, 모든 질문에 `n` 입력 (파일은 옮기지 않음). 확인 후 테스트 파일 삭제

| ID | 할 일 | 기대 결과 | 결과 |
|---|---|---|---|
| T10-4R | T10-4와 같은 오류 규칙을 맨 위에 추가한 config로 `err_test.txt`, `Lab01_test.txt`를 두고 실행 | `err_test.txt`만 제외되고 사유를 바로 알 수 있음, `Lab01_test.txt`는 정상 미리보기 | ✔ `(제외) err_test.txt : namePattern이 없는 규칙에서는 {1}을 쓸 수 없습니다 (rename에는 {name}, {ext}, 날짜만 가능)`, `Lab01_test.txt -> WebServerProgram\lab01.txt` |
| T11-8R | `Lab08_test.txt`를 두고 실행 → 기존 파일 질문이 떠 있는 동안 `Lab09_test.txt` 생성 → `n` | 감시 시작 직후 `Lab09_test.txt`만 승인 대기에 뜸. 이미 물어본 `Lab08_test.txt`는 다시 묻지 않음 | ✔ `[감시 시작]` 바로 뒤 `[새 파일 미리보기] 승인 대기 1개 / 1. Lab09_test.txt -> …\WebServerProgram\lab09.txt` |

### 발견한 문제와 수정
| # | 발견 방법 | 문제 | 원인 | 수정 방향 | 상태 |
|---|---|---|---|---|---|
| 1 | T10-4 | `namePattern` 없는 규칙에서 rename에 `{1}`을 쓰면 오류 메시지가 `namePattern에 괄호가 -1개` | 정규식이 없으면 `groups`가 빈 리스트라 `size() - 1 = -1` | `NameResolver.groupValue()`에서 `groups.isEmpty()`면 "namePattern이 없는 규칙에서는 {n}을 쓸 수 없습니다"로 따로 안내 | **수정함** (T10-4R로 확인) |
| 2 | T11-8 | 실행 직후 기존 파일 질문에 답하는 동안 받은 파일은 감시가 못 잡음 (다음 실행 때까지 Downloads에 남음) | 기존 파일 목록은 질문 전에 만들고, `WatchService` 등록은 답한 뒤라 그 사이 생긴 파일은 이벤트도 목록에도 없음 | 감시 시작 직후 폴더를 한 번 다시 읽어서, 처음 목록에 없던 파일만 `ApprovalQueue`에 넣기 → `Main.organizeExisting()`이 본 파일 목록을 돌려주고, `FolderWatcher`가 감시 등록 직후 `scanNewSinceStart()`로 그 목록에 없는 파일만 기존 경로(크기 안정 확인 → 승인 대기)로 보냄 | **수정함** (T11-8R로 확인) |
| 3 | T11-6 + 코드 읽기 | `HistoryLog` 주석은 "실시간 감시: 새 파일 하나하나가 각각 실행 1번"이라고 되어 있지만 실제로는 `y` 한 번에 승인한 묶음이 실행 1번 (`u` 한 번에 2건 되돌아옴) | 6단계에서 `ApprovalQueue`로 묶음 승인으로 바꾸면서 주석이 그대로 남음 | 주석을 "승인 한 번에 옮긴 묶음 = 실행 1번"으로 고치기 (README 설명은 이미 맞음) | **수정함** (`HistoryLog` 주석) |
| 4 | 다른 PC에서 실행해 보기 | README의 `lib/gson-2.11.0.jar`, `.iml`이 저장소에 없고, 폴더 배치 안내가 없어 받은 그대로는 실행 불가 | 패키지 폴더만 저장소로 올림 | [실행 방법](#실행-방법)에 준비 단계 추가 | **수정함** |
| 5 | T11-10a (실제 Downloads) | Downloads에 데이터통신 자료 5개가 있는데 하나도 안 잡힘 | 규칙은 `ch`로 **시작**하는 이름만 잡는데, 실제 파일은 `김우성_ch7. …`처럼 교수님 이름이 앞에 붙음. 테스트용 파일 이름(`ch2application_…`)만으로 규칙을 만들어서 놓침 | 이름 어디든 `ch숫자`면 잡도록 정규식 변경. `search2`처럼 영단어 끝의 `ch`는 제외 | **수정함** (T11-10b로 확인) |
| 6 | VS Code 터미널에서 README대로 실행 | README의 `java -cp out;lib/gson-2.11.0.jar …`로는 실행 안 됨 | 따옴표가 없어서 셸이 `;`를 명령 구분자로 읽어 `-cp` 값이 잘림 | `-cp "out;lib\gson-2.11.0.jar"`로 고치고 PowerShell용 명령, 한글 깨짐 옵션, [문제 해결](#문제-해결) 표 추가 | **수정함** |

## 변경 이력
| 시점 | 바꾼 것 | 종류 | 계기 | 판단 이유 |
|---|---|---|---|---|
| W09 | 패키지 5개(app, config, core, history, util)로 시작 | 설계 | 계획 | 역할별로 나눠서 단계마다 한 곳씩만 손대기 위해 |
| W10 · 1단계 | glob 4개 하드코딩 + if-else로 기존 파일 이동 | 구현 | 계획 | 최소 동작부터 확인하고 살 붙이기 |
| W10 · 2단계 | 충돌 처리 `(n)`, 임시 파일 무시, 파일마다 try-catch → **1차 검증** | 요구사항·구현 | 계획 | 여기까지만 있어도 정리기로 쓸 수 있는 최소 범위 |
| W10 · 3단계 | if문 하드코딩 → `config.json` + Gson | 설계 | 자기 점검: 규칙을 바꿀 때마다 코드를 고쳐야 함 | 비기능 요구사항(코드 수정 없이 규칙 변경) |
| W10 · 3단계 | `RuleMatcher`가 확장자뿐 아니라 glob도 지원 | 설계 | 자기 점검 | 계획은 "확장자만"이었지만 1단계 glob 규칙을 그대로 옮기려면 필요 |
| W11 · 4단계 | 정규식을 `RuleMatcher` 생성자에서 미리 컴파일 | 설계 | 자기 점검 | 정규식 문법 오류를 파일 처리 중이 아니라 실행 시작 때 발견 |
| W11 · 4단계 | `MovePlan`에 새 이름 필드, `FileMover.move` 오버로딩 | 설계 | 이름 변경 기능 | 기존 호출 코드는 그대로 두고 이름만 추가 |
| W11 · 5단계 | 감지 즉시 이동 → 크기 안정 + 쓰기 잠금 확인 후 처리 | 설계 | 주의 사항: 다운로드 직후 파일 잠김 | 바로 옮기면 이동 실패 |
| W11 · 5단계 | 파일별 예약 하나만 유지, 하위 폴더 이벤트 무시 | 설계 | 주의 사항: 이벤트 중복, 자기 자신 감시 | 같은 파일을 여러 번 처리하지 않기 위해 |
| W11 · 6단계 | **실행 흐름 변경** : 5단계 계획의 "모드 선택 메뉴(일괄 정리 / 감시)" → "기존 파일 미리보기·승인 → 실시간 감시"가 기본 흐름 | 사용법·설계 | 피드백: 실행할 때 작업할지, 자동 감지할지 | 둘 다 필요하고, 모든 이동은 승인 후에만 하는 게 안전 |
| W11 · 6단계 | 되돌린 경로 5초 감시 제외, y/n/u를 `runOnWorker()`로 실행 | 설계 | 자기 점검: 되돌린 파일을 감시가 다시 물어봄 | 되돌리기와 자동 정리가 같은 파일을 동시에 건드리지 않게 |
| W11 | 실험은 실제 Downloads에서 바로, 안전장치는 미리보기·승인·되돌리기로 | 사용법 | 피드백: 실험 단계에서 애로사항 예상 | 테스트 폴더를 따로 만들면 실제 파일 이름·다운로드 흐름을 확인할 수 없음(실제로 [발견 #5](#발견한-문제와-수정)는 실제 Downloads에서만 드러남). 잘못 옮겨도 승인 단계에서 막거나 `u`로 복구 가능 |
| 2026-11-21 / 11-22 | 저장소 생성(Initial commit) / 3~6단계 한 번에 커밋(`step3 to 6`) | 기록 | — | — (커밋이 단계별로 나뉘어 있지 않아 위 표로 단계별 변경을 대신 기록) |
| ____ (테스트 날짜) | 주차별 테스트 17개 실행 → 발견 #1~#4 기록 | 테스트 | 7단계 | 완료 기준을 실제로 확인하기 위해 |
| 2026-11-23 | `NameResolver` : `namePattern` 없는 규칙에서 `{n}`을 쓰면 원인을 바로 알려 주는 메시지로 변경 | 구현 | 테스트: [발견 #1](#발견한-문제와-수정) | 기존 메시지(`괄호가 -1개`)로는 무엇을 고쳐야 할지 알 수 없음. 조건 하나 추가로 해결 |
| 2026-11-23 | `FolderWatcher` 시작 직후 1회 폴더 검사 추가, `Main.organizeExisting()`이 본 파일 목록을 반환 | 설계 | 테스트: [발견 #2](#발견한-문제와-수정) | 감시 등록 전 공백 시간에 받은 파일이 다음 실행까지 남음. 이미 있던 `rescanAll()` 방식을 재사용하고, 이미 물어본 파일은 목록으로 걸러 다시 묻지 않게 함. 새 파일도 기존과 같은 경로(크기 안정 확인 → 승인 대기)를 거쳐 예외 처리를 새로 만들 필요 없음 |
| 2026-11-23 | `HistoryLog` 주석을 실제 동작(승인 묶음 = 실행 1번)에 맞게 수정 | 문서 | 자기 점검: [발견 #3](#발견한-문제와-수정) | 주석과 동작이 다르면 되돌리기 단위를 오해할 수 있음 |
| 2026-11-23 | README에 사용자·시나리오, 우선순위·완료 기준, 구성요소 관계·데이터·스레드 흐름, 실행 준비 단계, 테스트 기록, 변경 이력 추가 | 문서 | 평가 기준 자기 점검 | 다른 사람이 README만 보고 이해·실행할 수 있게 |
| 2026-11-22 ~ 23 | config 규칙 정리 : `lecture_(\d+)` → `데이터통신_n주차` 규칙 삭제, Java 규칙 `*Java*` → `*주차*강의자료*` | 요구사항 | ____ | ____ |
| 2026-11-23 | 이름 바꾸기 규칙 추가 : `*lab*` → `WebServerProgram/lab숫자`, `ch숫자…` → `데이터통신/ch_숫자`, `숫자주차…수업자료` → `고웹프/react_숫자` (glob → namePattern) | 요구사항 | 자기 점검: 옮겨도 원래 이름이 제각각이라 과목 폴더 안에서 다시 찾기 어려움 | 파일 이름 속 번호(lab·장·주차)로 이름을 통일하면 정렬만으로 순서대로 보임. 숫자를 뽑아야 해서 glob 대신 정규식 그룹 사용 |
| 2026-11-23 | 운영체제 규칙(`*김우성*`) 삭제, 운영체제 자료도 데이터통신 규칙으로 통일 | 요구사항 | 자기 점검 | 두 과목 교수님이 같아 자료 이름 패턴이 같음(`김우성_chN…`). 이름만으로 구분할 수 없어 한 폴더로 모음 |
| 2026-11-23 | 데이터통신 규칙 `^ch(\d+)` → `(?<![a-z])ch(\d+)` (이름 어디든 `ch숫자`) | 요구사항 | 테스트: 실제 Downloads에서 하나도 안 잡힘 ([발견 #5](#발견한-문제와-수정)) | 실제 파일은 `김우성_` 접두어가 붙어 있음. 앞을 영문자로 제한해 `search2` 같은 오탐은 막음 |
| 2026-11-23 | 실행 안내 개편 : 폴더 배치, Gson 받는 주소, PowerShell·Git Bash 명령, `-cp` 따옴표, 한글 깨짐 옵션, 문제 해결 표 | 사용법 | 테스트: README대로 실행이 안 됨 ([발견 #6](#발견한-문제와-수정)), IntelliJ 대신 VS Code로 실행하려다 막힘 | 다른 사람이 README만 보고 어떤 도구로든 실행할 수 있게 |
| 2026-11-23 | README에 문제 정의·해결 방법, 구현 범위(포함/제외), 실행 예시 추가 | 문서 | 평가 기준 자기 점검 | 누구의 어떤 문제를 어디까지 해결하는지 먼저 보이도록 |
