/**
 * Claude API 프록시.
 *
 * 앱에 API 키를 넣으면 APK를 디컴파일해 누구나 꺼낼 수 있다. 그래서 키는 여기(Secret Manager)에만 두고,
 * 앱은 Firebase 로그인 토큰만 보낸다. 요청 본문은 Anthropic Messages API 형식 그대로 받아 그대로 전달한다.
 *
 * 배포: firebase deploy --only functions
 * 키 설정: firebase functions:secrets:set ANTHROPIC_API_KEY
 */
const { onRequest } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const { initializeApp } = require("firebase-admin/app");
const { getAuth } = require("firebase-admin/auth");

initializeApp();
const ANTHROPIC_API_KEY = defineSecret("ANTHROPIC_API_KEY");

// 앱이 실제로 쓰는 모델과 출력 상한만 허용한다. 로그인한 사용자라도
// 비싼 모델·긴 출력으로 남용하지 못하게. 앱에서 모델을 바꾸면 여기도 같이 바꿔야 한다.
const ALLOWED_MODELS = new Set([
  "claude-haiku-4-5-20251001",
  "claude-sonnet-4-6",
  "claude-sonnet-5",
]);
const MAX_TOKENS_CAP = 8192;

// 오류도 Anthropic과 같은 모양으로 돌려준다. 앱은 원래 그 형식을 로그로 남기고 있다.
const error = (res, status, type, message) =>
  res.status(status).json({ type: "error", error: { type, message } });

exports.claude = onRequest(
  {
    region: "asia-northeast3", // 서울. 한국 사용자와의 왕복 시간을 줄인다.
    secrets: [ANTHROPIC_API_KEY],
    timeoutSeconds: 120, // 일기 생성(Sonnet, 긴 출력)이 기본 60초를 넘길 수 있다. 앱 readTimeout(90초)보다 길게.
  },
  async (req, res) => {
    if (req.method !== "POST") return error(res, 405, "invalid_request_error", "POST only");

    const idToken = (req.get("Authorization") || "").replace(/^Bearer /, "");
    try {
      await getAuth().verifyIdToken(idToken);
    } catch {
      return error(res, 401, "authentication_error", "로그인이 필요합니다");
    }

    const body = req.body;
    if (!ALLOWED_MODELS.has(body?.model)) {
      return error(res, 400, "invalid_request_error", `허용되지 않은 모델: ${body?.model}`);
    }
    if (!(body.max_tokens > 0 && body.max_tokens <= MAX_TOKENS_CAP)) {
      return error(res, 400, "invalid_request_error", `max_tokens는 1~${MAX_TOKENS_CAP}`);
    }

    const upstream = await fetch("https://api.anthropic.com/v1/messages", {
      method: "POST",
      headers: {
        "content-type": "application/json",
        "x-api-key": ANTHROPIC_API_KEY.value(),
        "anthropic-version": "2023-06-01",
      },
      body: JSON.stringify(body),
    });
    res.status(upstream.status).type("application/json").send(await upstream.text());
  }
);
