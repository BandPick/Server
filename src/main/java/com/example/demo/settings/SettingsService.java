package com.example.demo.settings;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SettingsService {

    private final SettingsRepository settingsRepository;

    public SettingsService(SettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    @Transactional
    public SettingsResponse getSettings() {
        Settings settings = settingsRepository.findTopByOrderByIdAsc()
                .orElseGet(this::createDefaultSettings);

        return toResponse(settings);
    }

    /**
     * id가 가장 작은 settings 행 하나만 조회해 필드를 갱신합니다.
     * 행이 없으면 요청 값으로 한 행을 INSERT 합니다.
     * 과거에 남은 중복 행이 있으면 갱신한 행만 남기고 삭제합니다.
     */
    @Transactional
    public SettingsResponse saveSettings(SettingsRequest request) {
        Settings row = settingsRepository.findTopByOrderByIdAsc()
                .orElseGet(Settings::new);

        row.setDeadline(request.deadline());
        row.setMinVocalSongs(request.minVocalSongs());
        row.setMinSessionSongs(request.minSessionSongs());
        row.setUpdateTime(LocalDateTime.now());

        Settings saved = settingsRepository.save(row);
        settingsRepository.deleteAllExceptId(saved.getId());
        return toResponse(saved);
    }

    private Settings createDefaultSettings() {
        Settings settings = new Settings();
        settings.setDeadline(LocalDateTime.now().plusDays(7));
        settings.setMinVocalSongs(0);
        settings.setMinSessionSongs(0);
        settings.setUpdateTime(LocalDateTime.now());
        return settingsRepository.save(settings);
    }

    private SettingsResponse toResponse(Settings settings) {
        return new SettingsResponse(
                settings.getId(),
                settings.getDeadline(),
                settings.getMinVocalSongs(),
                settings.getMinSessionSongs(),
                settings.getUpdateTime()
        );
    }
}
