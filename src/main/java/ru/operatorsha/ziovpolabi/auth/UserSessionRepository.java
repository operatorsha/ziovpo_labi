package ru.operatorsha.ziovpolabi.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import ru.operatorsha.ziovpolabi.user.UserAccount;

import java.util.Optional;
import java.util.UUID;

public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    Optional<UserSession> findByRefreshTokenHashAndStatus(String refreshTokenHash, SessionStatus status);

    Optional<UserSession> findByRefreshTokenHash(String refreshTokenHash);

    @Modifying
    @Query("update UserSession s set s.status = :newStatus where s.user = :user and s.status = 'ACTIVE'")
    void revokeActiveSessions(UserAccount user, SessionStatus newStatus);
}
