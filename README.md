# Java Personal Project <hr>
## Subject
Problem : 학년이 올라갈수록 기존의 것과 더불어 파일 관리가 힘들어짐.       
또 지금 학원 조교일을 하는데 하루에 다운받는 파일만 수십 개에 관리도 잘 안 돼서 같은 파일 여러 개 다운받고 찾기도 힘듦-> 작업 효율 떡락   
찾고자 하는 파일들을 못 찾으면 화가 남, 자주 쓰는데 정리 안 된 거 보면 짜증남.  

Selection Criteria : 
1. 과한 모듈 사용 X
2. 다양한 문법 사용
3. 난이도 너무 높으면 X
4. 구현 과정이 직관적이었음 좋겠다   

Solution : 다운로드 파일의 주 경로인 Download 폴더를 감시, 이하는 Function Requirements의 순서대로
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

## 실행 방법
IntelliJ에서 `app/Main` 실행 (기본 설정 파일: `src/Java_Project/config.json`, 실행 인자로 다른 경로 지정 가능)   
외부 라이브러리 : `lib/gson-2.11.0.jar` (JSON 읽기용, 모듈에 등록됨)

```
1. 기존 파일 미리보기   Downloads에 이미 있는 파일이 어디로 갈지 번호 붙여 출력
                       → y: 이동 / n: 그대로 두기
2. 실시간 감시 시작     q 입력할 때까지 계속
   새 파일 감지 시      미리보기 출력 → y: 이동 / n: 건너뛰기
   u                   마지막 작업 되돌리기 (누를 때마다 하나씩 이전으로)
   q                   종료
```

### 규칙 작성 (config.json)
```json
{
  "watchDir": "C:\\Users\\likgr\\Downloads",
  "baseDir": "C:\\Users\\likgr\\Desktop\\--",
  "rules": [
    {
      "name": "데이터통신 강의",
      "match": { "extensions": ["pdf", "pptx"], "namePattern": "^lecture_(\\d+)" },
      "target": "데이터통신",
      "rename": "데이터통신_{1}주차.{ext}"
    },
    { "name": "운영체제", "match": { "glob": "*김우성*" }, "target": "운영체제" }
  ]
}
```
| 항목 | 설명 |
|---|---|
| `watchDir` | 감시할 폴더 |
| `baseDir` | `target`이 상대경로일 때 기준 폴더 |
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
| `core/` | `FolderWatcher` | `WatchService`로 이벤트 수신, 다운로드 완료 대기, 중복 이벤트 제거 |
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

## 구현 단계
1. 하드코딩한 규칙 하나로 폴더 내 기존 파일을 확장자별 이동 | Files.list, Files.move
2. 충돌 처리, 임시 파일 무시, 예외 처리 | 예외 설계, Path 다루기   
-> 여기까지만 해도 작동. 검증후 이후 단계로 살 붙여나가기. **1차 검증**
3. 규칙을 Json으로 분리 | 객체 매핑
4. 정규식 매칭과 이름 패턴 치환 | Pattern, Matcher 그룹
5. 실시간 감시 모드 | WatchService, 스레드
6. 미리보기, 승인, 로그, 되돌리기 | 설계 확장, 파일 기록
7. 트레이 아이콘이나 간단한 GUI, JUnit 테스트 | Swing/JavaFX, 테스트  

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

**확인: lecture_03.pdf → 데이터통신_03주차.pdf로 바뀌어 이동**

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
NameResolver, RuleMatcher부터 JUnit 테스트 작성
여유 있으면 트레이 아이콘이나 간단한 Swing 창

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
* 구현 과정은 어렵지 않을 것으로 예상, 근데 실험 단계에서 애로사항이 있을 거임   
  → 실험은 실제 Downloads 대신 임시 폴더를 가리키는 별도 config로 진행 (실행 인자로 config 경로 지정)
* 프로그램 실행시 일련의 작업을 할 것인지, 실행 안 해도 자동으로 감지해서 시행할 것인지.   
  → 둘 다 : 실행하면 기존 파일을 미리보기·승인으로 정리하고, 이어서 끄기 전까지 실시간 감시. 모든 이동은 승인 후에만
