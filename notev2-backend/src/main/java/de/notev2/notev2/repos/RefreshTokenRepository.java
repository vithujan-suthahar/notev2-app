package de.notev2.notev2.repos;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import de.notev2.notev2.entity.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);
}
