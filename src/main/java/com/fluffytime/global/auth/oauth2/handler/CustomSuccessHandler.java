package com.fluffytime.global.auth.oauth2.handler;

import static com.fluffytime.global.auth.jwt.util.constants.TokenName.ACCESS_TOKEN_NAME;
import static com.fluffytime.global.auth.jwt.util.constants.TokenName.REFRESH_TOKEN_NAME;

import com.fluffytime.global.auth.jwt.dao.RefreshTokenDao;
import com.fluffytime.global.auth.jwt.util.JwtTokenizer;
import com.fluffytime.global.auth.oauth2.dao.SocialTempUserDao;
import com.fluffytime.global.auth.oauth2.dto.CustomOAuth2User;
import com.fluffytime.global.auth.oauth2.dto.SocialTempUser;
import com.fluffytime.global.auth.jwt.util.TokenCookieManager;
import com.fluffytime.global.auth.jwt.util.constants.TokenExpiry;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenizer jwtTokenizer;
    private final SocialTempUserDao socialTempUserDao;
    private final RefreshTokenDao refreshTokenDao;

    // auth 인증 성공 시 토큰 쿠기 발행 메서드
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
        Authentication authentication) throws IOException, ServletException {

        CustomOAuth2User customOAuth2User = (CustomOAuth2User) authentication.getPrincipal();

        Long userId = customOAuth2User.getUserId();
        String email = customOAuth2User.getName();
        String nickname = customOAuth2User.getNickname();
        List<String> roles = customOAuth2User.getRoles();

        SocialTempUser socialTempUser = socialTempUserDao.getSocialTempUser(email).orElse(null);

        if (socialTempUser == null) {

            //토큰 발급
            String accessToken = jwtTokenizer.createAccessToken(userId, email, nickname, roles);
            String refreshToken = jwtTokenizer.createRefreshToken(userId, email, nickname, roles);

            refreshTokenDao.saveRefreshToken(email, refreshToken);

            Cookie accessTokenCookie = TokenCookieManager.generateTokenCookie(
                ACCESS_TOKEN_NAME.getName(),
                accessToken,
                TokenExpiry.ACCESS_TOKEN_EXPIRY_SECOND.getExpiry()
            );

            Cookie refreshTokenCookie = TokenCookieManager.generateTokenCookie(
                REFRESH_TOKEN_NAME.getName(),
                refreshToken,
                TokenExpiry.REFRESH_TOKEN_EXPIRY_SECOND.getExpiry()
            );

            response.addCookie(accessTokenCookie);
            response.addCookie(refreshTokenCookie);

            response.sendRedirect("/");
        } else {
            // 추가 정보 기입 페이지로 이동
            response.sendRedirect("/join/social?email="+email);
        }


    }
}
