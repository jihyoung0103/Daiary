package com.smu.daiary.data.source

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

/** functions/index.js가 배포된 주소. 리전·프로젝트가 바뀌면 같이 바꾼다. */
internal const val FUNCTIONS_BASE_URL = "https://asia-northeast3-daiary-58328.cloudfunctions.net"

/**
 * 우리 서버 프록시(Claude·날씨)에 붙일 Firebase 로그인 토큰.
 * 외부 API 키는 앱에 없고 서버에만 있다 — 앱은 "누구인지"만 증명한다.
 *
 * Authorization 헤더가 아니라 이 헤더로 보낸다. Authorization에 JWT를 넣으면
 * Cloud Run이 자기 IAM 토큰으로 오인해 함수에 닿기 전에 거부한다.
 */
internal const val PROXY_TOKEN_HEADER = "X-Firebase-Token"

/** 토큰은 1시간짜리라 SDK가 캐시하고, 만료됐을 때만 새로 받아온다. */
internal suspend fun proxyIdToken(): String =
    FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token
        ?: throw IllegalStateException("로그인하지 않아 서버 프록시를 호출할 수 없음")
