from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from openai import AsyncOpenAI
import os
import json
from pathlib import Path
from typing import Optional, Any, Dict
from dotenv import load_dotenv

load_dotenv()

app = FastAPI(
    title="YumCoach Chatbot API",
    version="1.0.0"
)

client = AsyncOpenAI(
    base_url="https://gms.ssafy.io/gmsapi/api.openai.com/v1",
    api_key=os.getenv("OPENAI_API_KEY")
)

def load_prompt(filename: str) -> str:
    """프롬프트 파일 로드"""
    prompt_path = Path(__file__).parent / "prompts" / filename
    try:
        with open(prompt_path, 'r', encoding='utf-8') as f:
            content = f.read()
            print(f"✅ 프롬프트 파일 로드 성공: {filename} ({len(content)} chars)")
            return content
    except FileNotFoundError:
        print(f"❌ Warning: {filename} not found")
        return ""

class ChatRequest(BaseModel):
    message: str
    user_id: str = None

class ChatResponse(BaseModel):
    reply: str
    detected_hashtag: str = None

class AnalyzeReportRequest(BaseModel):
    report: Any

@app.post("/analyze-report")
async def analyze_report(req: AnalyzeReportRequest):
    try:
        report_json = req.report

        # Diagnostic: print top-level keys and meals payload to debug missing meals issue
        try:
            print("[DEBUG] Received analyze-report payload keys:", list(report_json.keys()) if isinstance(report_json, dict) else type(report_json))
            if isinstance(report_json, dict):
                meals_raw = None
                for key in ("meals", "reportMeals", "report_meals", "dailyMeals"):
                    if key in report_json:
                        meals_raw = report_json.get(key)
                        print(f"[DEBUG] Found meals key '{key}' type={type(meals_raw).__name__}")
                        break
                if meals_raw is None:
                    print("[DEBUG] No meals key found in incoming report payload")
                else:
                    try:
                        # if it's a stringified JSON array, try parsing for inspection
                        if isinstance(meals_raw, str):
                            parsed_meals_preview = json.loads(meals_raw)
                        else:
                            parsed_meals_preview = meals_raw
                        if isinstance(parsed_meals_preview, list):
                            print(f"[DEBUG] meals length={len(parsed_meals_preview)}; first_items={parsed_meals_preview[:5]}")
                        else:
                            print(f"[DEBUG] meals present but not a list: {type(parsed_meals_preview).__name__}")
                    except Exception as e:
                        print(f"[DEBUG] Failed to parse meals_raw: {e}; raw_preview={str(meals_raw)[:500]}")
        except Exception as e:
            print(f"[DEBUG] Error while logging incoming payload: {e}")

        if isinstance(report_json, str):
            try:
                report_json = json.loads(report_json)
            except Exception:
                pass

        system_prompt = load_prompt("report_analysis_prompt.txt")
        
        # 🔥 프롬프트 파일이 제대로 로드됐는지 확인
        if not system_prompt or len(system_prompt) < 100:
            print(f"❌ 프롬프트 파일이 비어있거나 너무 짧습니다: {len(system_prompt)} chars")
            raise HTTPException(status_code=500, detail="프롬프트 파일 로드 실패")

        # ===== 사용자 정보 추출 =====
        name = ""
        height = ""
        weight = ""
        activity_level = ""
        age = ""
        dietary_restrictions = ""
        health_status = ""

        try:
            if isinstance(report_json, dict):
                user_obj = None
                if "user" in report_json and isinstance(report_json["user"], dict):
                    user_obj = report_json["user"]
                elif "userInfo" in report_json and isinstance(report_json["userInfo"], dict):
                    user_obj = report_json["userInfo"]
                else:
                    user_obj = report_json

                name = user_obj.get("name", "") if isinstance(user_obj, dict) else ""
                height = user_obj.get("height", "") if isinstance(user_obj, dict) else ""
                weight = user_obj.get("weight", "") if isinstance(user_obj, dict) else ""
                activity_level = user_obj.get("activity_level", "") if isinstance(user_obj, dict) else user_obj.get("activityLevel", "") if isinstance(user_obj, dict) else ""
                age = user_obj.get("age", "") if isinstance(user_obj, dict) else ""

                dr = user_obj.get("dietary_restrictions", None) if isinstance(user_obj, dict) else None
                if dr is None:
                    dr = user_obj.get("dietaryRestrictions", None) if isinstance(user_obj, dict) else None
                if isinstance(dr, list):
                    dietary_restrictions = ", ".join([str(x) for x in dr])
                elif dr is not None:
                    dietary_restrictions = str(dr)

                hs = user_obj.get("health_status", None) if isinstance(user_obj, dict) else None
                if hs is None:
                    hs = user_obj.get("healthStatus", None) if isinstance(user_obj, dict) else None
                if isinstance(hs, dict):
                    vals = []
                    for k, v in hs.items():
                        if v:
                            vals.append(k)
                    health_status = ", ".join(vals) if vals else "건강함"
                elif hs is not None:
                    health_status = str(hs)
                else:
                    health_status = "건강함"
        except Exception as e:
            print(f"사용자 정보 추출 실패: {e}")

        # 프롬프트 플레이스홀더 치환
        try:
            if system_prompt:
                system_prompt = system_prompt.replace("{name}", str(name))
                system_prompt = system_prompt.replace("{height}", str(height))
                system_prompt = system_prompt.replace("{weight}", str(weight))
                system_prompt = system_prompt.replace("{activity_level}", str(activity_level))
                system_prompt = system_prompt.replace("{age}", str(age))
                system_prompt = system_prompt.replace("{dietary_restrictions}", str(dietary_restrictions))
                system_prompt = system_prompt.replace("{health_status}", str(health_status))
                
                # 🔥 치환 후 확인
                print(f"✅ 프롬프트 치환 완료 - 이름:{name}, 키:{height}, 체중:{weight}, 활동량:{activity_level}")
        except Exception as e:
            print(f"프롬프트 치환 실패: {e}")

        # ===== 영양소 요약 생성 =====
        nutrition_summary = ""
        try:
            if isinstance(report_json, dict):
                total_cal = report_json.get("totalCalories", 0)
                total_protein = report_json.get("proteinG", 0)
                total_carbs = report_json.get("carbG", 0)
                total_fat = report_json.get("fatG", 0)
                meal_count = report_json.get("mealCount", 0)

                # 기간 라벨 결정 (DAILY / WEEKLY 등)
                period_label = "오늘의"
                try:
                    rtype = report_json.get("type", "").upper() if isinstance(report_json, dict) else ""
                    if rtype == "WEEKLY" or (report_json.get("fromDate") and report_json.get("toDate")):
                        period_label = "이번 주의"
                    elif rtype == "MONTHLY":
                        period_label = "이번 달의"
                except Exception:
                    period_label = ""

                nutrition_summary = f"""## {period_label} 영양소 요약
```
총 칼로리: {total_cal} kcal
단백질: {total_protein} g
탄수화물: {total_carbs} g
지방: {total_fat} g
식사 횟수: {meal_count} 회
```
"""
                print(f"영양소 요약: 칼로리={total_cal}, 단백질={total_protein}, 탄={total_carbs}, 지방={total_fat} (label={period_label})")
        except Exception as e:
            print(f"영양소 요약 생성 실패: {e}")

        # ===== 식단 상세 내역 생성 =====
        meal_details = ""
        try:
            meals = None
            if isinstance(report_json, dict):
                # 여러 가능한 키를 허용하고, 문자열로 된 JSON도 파싱합니다.
                for key in ("meals", "reportMeals", "report_meals", "dailyMeals"):
                    if key in report_json and report_json.get(key) is not None:
                        meals = report_json.get(key)
                        break

                # meals가 JSON 문자열인 경우 파싱 시도
                if isinstance(meals, str):
                    try:
                        meals = json.loads(meals)
                    except Exception:
                        meals = None

                if meals and isinstance(meals, list) and len(meals) > 0:
                    meal_details = "\n## 식단 상세 내역\n\n"
                    for i, meal in enumerate(meals, 1):
                        if not isinstance(meal, dict):
                            continue
                        meal_name = meal.get("mealName") or meal.get("name") or meal.get("label") or f"식사 {i}"
                        calories = meal.get("calories") or meal.get("cal") or meal.get("kcal") or 0
                        protein = meal.get("proteinG") or meal.get("protein") or 0
                        carbs = meal.get("carbG") or meal.get("carbs") or meal.get("carb") or 0
                        fat = meal.get("fatG") or meal.get("fat") or 0

                        meal_details += f"### {i}. {meal_name}\n"
                        meal_details += f"- 칼로리: {calories}kcal\n"
                        meal_details += f"- 단백질: {protein}g, 탄수화물: {carbs}g, 지방: {fat}g\n\n"
                    print(f"✅ 식단 상세: {len(meals)}개 식사")
                else:
                    print("ℹ️ 식단 배열이 없거나 비어있습니다; meal_details 생략")
        except Exception as e:
            print(f"식단 상세 생성 실패: {e}")

        # ===== 챌린지 컨텍스트 생성 =====
        challenge_context = ""
        try:
            if isinstance(report_json, dict) and "activeChallenges" in report_json:
                active_challenges = report_json["activeChallenges"]
                if active_challenges and len(active_challenges) > 0:
                    challenge_context = "\n## 🎯 진행 중인 챌린지\n\n"
                    
                    for i, ch in enumerate(active_challenges, 1):
                        challenge_id = ch.get('challengeId', ch.get('id', 'N/A'))
                        title = ch.get('title', 'N/A')
                        goal_type = ch.get('goalType', 'N/A')
                        current_streak = ch.get('currentStreak', 0)
                        max_streak = ch.get('maxStreak', 0)
                        start_date = ch.get('startDate', 'N/A')
                        end_date = ch.get('endDate', 'N/A')
                        
                        goal_details = ch.get('goalDetails', '{}')
                        if isinstance(goal_details, str):
                            try:
                                goal_details = json.loads(goal_details)
                            except:
                                pass
                        
                        challenge_context += f"### 챌린지 {i}: {title}\n"
                        challenge_context += f"```\n"
                        challenge_context += f"ID: {challenge_id}\n"
                        challenge_context += f"타입: {goal_type}\n"
                        challenge_context += f"목표: {json.dumps(goal_details, ensure_ascii=False)}\n"
                        challenge_context += f"연속 달성: {current_streak}일 (최고 {max_streak}일)\n"
                        challenge_context += f"기간: {start_date} ~ {end_date}\n"
                        challenge_context += f"```\n\n"
                    print(f"✅ 챌린지: {len(active_challenges)}개")
        except Exception as e:
            print(f"챌린지 컨텍스트 생성 실패: {e}")

        # ===== user_content 구성 =====
        # 분석 대상 문구에 기간을 포함하여 "오늘" 표현이 주간 리포트에 나오지 않도록 함
        try:
            period_intro = "다음 리포트를 분석하세요:"
            if isinstance(report_json, dict):
                rtype = report_json.get("type", "").upper()
                if rtype == "DAILY":
                    period_intro = "일간 식단을 분석해 리포트를 작성하세요."
                elif rtype == "WEEKLY":
                    period_intro = "주간 식단을 모두 분석해 리포트를 작성하세요."
        except Exception:
            period_intro = "다음 식단들을  분석하세요:"

        user_content = f"""{period_intro}

{nutrition_summary}

{meal_details}

{challenge_context}

## 전체 리포트 데이터
```json
{json.dumps(report_json, ensure_ascii=False, indent=2)}
```
"""

        # ===== OpenAI API 호출 =====
        messages = [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_content}
        ]

        # Diagnostic: print a preview of the user_content to verify meal_details presence
        try:
            preview = user_content if len(user_content) <= 3000 else user_content[:3000] + "\n...[TRUNCATED]"
            print("[DEBUG] user_content preview start:\n" + preview + "\n[DEBUG] user_content preview end")
            if "## 식단 상세 내역" in user_content:
                print("[DEBUG] user_content contains '## 식단 상세 내역'")
            else:
                print("[DEBUG] user_content DOES NOT contain '## 식단 상세 내역'")
        except Exception as e:
            print(f"[DEBUG] Failed to print user_content preview: {e}")

        print(f"🤖 OpenAI API 호출 시작...")
        stream = await client.chat.completions.create(
            model=os.getenv("OPENAI_MODEL", "gpt-5-nano"),
            messages=messages,
            stream=False,
        )

        content = stream.choices[0].message.content
        print(f"✅ OpenAI 응답 받음: {len(content)} chars")
        
        if "```json" in content:
            content = content.split("```json")[1].split("```")[0].strip()
        elif "```" in content:
            content = content.split("```")[1].split("```")[0].strip()

        parsed = json.loads(content)
        
        # 🔥 응답 확인 로깅
        print(f"\n✅ AI 응답 파싱 성공:")
        print(f"- heroTitle: {parsed.get('heroTitle', '❌ MISSING')}")
        print(f"- heroLine: {parsed.get('heroLine', '❌ MISSING')}")
        print(f"- coachMessage: {parsed.get('coachMessage', '❌ MISSING')[:50] if parsed.get('coachMessage') else '❌ MISSING'}...")
        print(f"- nextAction: {parsed.get('nextAction', '❌ MISSING')[:50] if parsed.get('nextAction') else '❌ MISSING'}...")
        print(f"- score: {parsed.get('score', '❌ MISSING')}")
        print(f"- insights count: {len(parsed.get('insights', []))}\n")
        
        if "score" not in parsed:
            print("⚠️  score 없음, 기본값 50 설정")
            parsed["score"] = 50
        if "insights" not in parsed or len(parsed["insights"]) != 3:
            print(f"⚠️  insights가 3개가 아님: {len(parsed.get('insights', []))}개")
        
        return parsed

    except json.JSONDecodeError as e:
        print(f"❌ JSON 파싱 오류: {e}")
        print(f"응답 내용: {content[:500]}...")
        raise HTTPException(status_code=500, detail=f"AI 응답 파싱 실패: {str(e)}")
    except Exception as e:
        print(f"❌ 분석 오류: {e}")
        import traceback
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=str(e))


# 실행: uvicorn main:app --host 0.0.0.0 --port 8000