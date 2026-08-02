package com.fisbu.api.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public class SaveSplitRequest {

    @NotEmpty(message = "En az 2 katılımcı olmalıdır")
    @Size(min = 2, message = "En az 2 katılımcı olmalıdır")
    @Valid
    private List<SplitParticipantDto> participants;

    public List<SplitParticipantDto> getParticipants() { return participants; }
    public void setParticipants(List<SplitParticipantDto> participants) { this.participants = participants; }
}
