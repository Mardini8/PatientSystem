package com.journalSystem.message_service.service;

import com.journalSystem.message_service.model.Message;
import com.journalSystem.message_service.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @InjectMocks
    private MessageService messageService;

    private Message testMessage;
    private LocalDateTime testTime;

    @BeforeEach
    void setUp() {
        testTime = LocalDateTime.now();
        testMessage = createTestMessage(1L, 100L, 200L, "197001011234",
                "Test message content", testTime);
    }

    // send() TESTS

    @Test
    void send_shouldSaveAndReturnMessage_whenValidMessage() {
        // Arrange
        Message messageToSend = createTestMessage(null, 100L, 200L, "197001011234",
                "New message", testTime);
        Message savedMessage = createTestMessage(1L, 100L, 200L, "197001011234",
                "New message", testTime);

        when(messageRepository.save(messageToSend)).thenReturn(savedMessage);

        // Act
        Message result = messageService.send(messageToSend);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getFromUserId()).isEqualTo(100L);
        assertThat(result.getToUserId()).isEqualTo(200L);
        assertThat(result.getContent()).isEqualTo("New message");

        verify(messageRepository, times(1)).save(messageToSend);
    }

    @Test
    void send_shouldAssignId_whenMessageHasNoId() {
        // Arrange
        Message messageWithoutId = createTestMessage(null, 100L, 200L, "197001011234",
                "Message without ID", testTime);
        Message savedMessage = createTestMessage(42L, 100L, 200L, "197001011234",
                "Message without ID", testTime);

        when(messageRepository.save(messageWithoutId)).thenReturn(savedMessage);

        // Act
        Message result = messageService.send(messageWithoutId);

        // Assert
        assertThat(result.getId()).isEqualTo(42L);
        verify(messageRepository).save(messageWithoutId);
    }

    @Test
    void send_shouldHandleLongContent() {
        // Arrange
        String longContent = "A".repeat(3000);
        Message messageWithLongContent = createTestMessage(null, 100L, 200L, "197001011234",
                longContent, testTime);
        Message savedMessage = createTestMessage(1L, 100L, 200L, "197001011234",
                longContent, testTime);

        when(messageRepository.save(messageWithLongContent)).thenReturn(savedMessage);

        // Act
        Message result = messageService.send(messageWithLongContent);

        // Assert
        assertThat(result.getContent()).hasSize(3000);
        verify(messageRepository).save(messageWithLongContent);
    }

    @Test
    void send_shouldPreserveTimestamp() {
        // Arrange
        LocalDateTime specificTime = LocalDateTime.of(2024, 1, 15, 14, 30);
        Message messageWithTime = createTestMessage(null, 100L, 200L, "197001011234",
                "Timed message", specificTime);
        Message savedMessage = createTestMessage(1L, 100L, 200L, "197001011234",
                "Timed message", specificTime);

        when(messageRepository.save(messageWithTime)).thenReturn(savedMessage);

        // Act
        Message result = messageService.send(messageWithTime);

        // Assert
        assertThat(result.getSentAt()).isEqualTo(specificTime);
        verify(messageRepository).save(messageWithTime);
    }

    @Test
    void send_shouldHandleEmptyContent() {
        // Arrange
        Message messageWithEmptyContent = createTestMessage(null, 100L, 200L, "197001011234",
                "", testTime);
        Message savedMessage = createTestMessage(1L, 100L, 200L, "197001011234",
                "", testTime);

        when(messageRepository.save(messageWithEmptyContent)).thenReturn(savedMessage);

        // Act
        Message result = messageService.send(messageWithEmptyContent);

        // Assert
        assertThat(result.getContent()).isEmpty();
        verify(messageRepository).save(messageWithEmptyContent);
    }

    @Test
    void send_shouldHandleNullContent() {
        // Arrange
        Message messageWithNullContent = createTestMessage(null, 100L, 200L, "197001011234",
                null, testTime);
        Message savedMessage = createTestMessage(1L, 100L, 200L, "197001011234",
                null, testTime);

        when(messageRepository.save(messageWithNullContent)).thenReturn(savedMessage);

        // Act
        Message result = messageService.send(messageWithNullContent);

        // Assert
        assertThat(result.getContent()).isNull();
        verify(messageRepository).save(messageWithNullContent);
    }

    @Test
    void send_shouldHandleSpecialCharactersInContent() {
        // Arrange
        String specialContent = "Test åäö ÅÄÖ @#$%^&* 😀 <html> & \"quotes\"";
        Message messageWithSpecialChars = createTestMessage(null, 100L, 200L, "197001011234",
                specialContent, testTime);
        Message savedMessage = createTestMessage(1L, 100L, 200L, "197001011234",
                specialContent, testTime);

        when(messageRepository.save(messageWithSpecialChars)).thenReturn(savedMessage);

        // Act
        Message result = messageService.send(messageWithSpecialChars);

        // Assert
        assertThat(result.getContent()).isEqualTo(specialContent);
        verify(messageRepository).save(messageWithSpecialChars);
    }

    // forPatient() TESTS

    @Test
    void forPatient_shouldReturnMessages_whenMessagesExist() {
        // Arrange
        String patientPersonnummer = "197001011234";
        List<Message> expectedMessages = new ArrayList<>();
        expectedMessages.add(createTestMessage(1L, 100L, 200L, patientPersonnummer, "Message 1", testTime));
        expectedMessages.add(createTestMessage(2L, 200L, 100L, patientPersonnummer, "Message 2", testTime.minusHours(1)));

        when(messageRepository.findByPatientPersonnummerOrderBySentAtDesc(patientPersonnummer))
                .thenReturn(expectedMessages);

        // Act
        List<Message> result = messageService.forPatient(patientPersonnummer);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getPatientPersonnummer()).isEqualTo(patientPersonnummer);
        assertThat(result.get(1).getPatientPersonnummer()).isEqualTo(patientPersonnummer);

        verify(messageRepository).findByPatientPersonnummerOrderBySentAtDesc(patientPersonnummer);
    }

    @Test
    void forPatient_shouldReturnEmptyList_whenNoMessagesExist() {
        // Arrange
        String patientPersonnummer = "199999999999";

        when(messageRepository.findByPatientPersonnummerOrderBySentAtDesc(patientPersonnummer))
                .thenReturn(new ArrayList<>());

        // Act
        List<Message> result = messageService.forPatient(patientPersonnummer);

        // Assert
        assertThat(result).isEmpty();
        verify(messageRepository).findByPatientPersonnummerOrderBySentAtDesc(patientPersonnummer);
    }

    @Test
    void forPatient_shouldReturnMessagesInDescendingOrder() {
        // Arrange
        String patientPersonnummer = "197001011234";
        LocalDateTime now = LocalDateTime.now();

        List<Message> messages = new ArrayList<>();
        messages.add(createTestMessage(3L, 100L, 200L, patientPersonnummer, "Latest", now));
        messages.add(createTestMessage(2L, 100L, 200L, patientPersonnummer, "Middle", now.minusHours(1)));
        messages.add(createTestMessage(1L, 100L, 200L, patientPersonnummer, "Oldest", now.minusHours(2)));

        when(messageRepository.findByPatientPersonnummerOrderBySentAtDesc(patientPersonnummer))
                .thenReturn(messages);

        // Act
        List<Message> result = messageService.forPatient(patientPersonnummer);

        // Assert
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getContent()).isEqualTo("Latest");
        assertThat(result.get(1).getContent()).isEqualTo("Middle");
        assertThat(result.get(2).getContent()).isEqualTo("Oldest");
    }

    @Test
    void forPatient_shouldHandleNullPersonnummer() {
        // Arrange
        when(messageRepository.findByPatientPersonnummerOrderBySentAtDesc(null))
                .thenReturn(new ArrayList<>());

        // Act
        List<Message> result = messageService.forPatient(null);

        // Assert
        assertThat(result).isEmpty();
        verify(messageRepository).findByPatientPersonnummerOrderBySentAtDesc(null);
    }

    @Test
    void forPatient_shouldHandleMultipleMessagesFromDifferentUsers() {
        // Arrange
        String patientPersonnummer = "197001011234";
        List<Message> messages = new ArrayList<>();
        messages.add(createTestMessage(1L, 100L, 200L, patientPersonnummer, "From user 100", testTime));
        messages.add(createTestMessage(2L, 200L, 100L, patientPersonnummer, "From user 200", testTime.minusMinutes(5)));
        messages.add(createTestMessage(3L, 300L, 100L, patientPersonnummer, "From user 300", testTime.minusMinutes(10)));

        when(messageRepository.findByPatientPersonnummerOrderBySentAtDesc(patientPersonnummer))
                .thenReturn(messages);

        // Act
        List<Message> result = messageService.forPatient(patientPersonnummer);

        // Assert
        assertThat(result).hasSize(3);
        assertThat(result.stream().map(Message::getFromUserId))
                .containsExactly(100L, 200L, 300L);
    }

    @Test
    void forPatient_shouldHandleDifferentPersonnummerFormats() {
        // Arrange
        String[] personnummerFormats = {
                "197001011234",
                "19700101-1234",
                "700101-1234",
                "7001011234"
        };

        for (String personnummer : personnummerFormats) {
            List<Message> messages = List.of(
                    createTestMessage(1L, 100L, 200L, personnummer, "Test", testTime)
            );

            when(messageRepository.findByPatientPersonnummerOrderBySentAtDesc(personnummer))
                    .thenReturn(messages);

            // Act
            List<Message> result = messageService.forPatient(personnummer);

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getPatientPersonnummer()).isEqualTo(personnummer);
        }
    }

    // fromUser() TESTS

    @Test
    void fromUser_shouldReturnMessages_whenMessagesExist() {
        // Arrange
        Long userId = 100L;
        List<Message> expectedMessages = new ArrayList<>();
        expectedMessages.add(createTestMessage(1L, userId, 200L, "197001011234", "Message 1", testTime));
        expectedMessages.add(createTestMessage(2L, userId, 300L, "198001011234", "Message 2", testTime.minusHours(1)));

        when(messageRepository.findByFromUserIdOrderBySentAtDesc(userId))
                .thenReturn(expectedMessages);

        // Act
        List<Message> result = messageService.fromUser(userId);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getFromUserId()).isEqualTo(userId);
        assertThat(result.get(1).getFromUserId()).isEqualTo(userId);

        verify(messageRepository).findByFromUserIdOrderBySentAtDesc(userId);
    }

    @Test
    void fromUser_shouldReturnEmptyList_whenNoMessagesExist() {
        // Arrange
        Long userId = 999L;

        when(messageRepository.findByFromUserIdOrderBySentAtDesc(userId))
                .thenReturn(new ArrayList<>());

        // Act
        List<Message> result = messageService.fromUser(userId);

        // Assert
        assertThat(result).isEmpty();
        verify(messageRepository).findByFromUserIdOrderBySentAtDesc(userId);
    }

    @Test
    void fromUser_shouldReturnMessagesInDescendingOrder() {
        // Arrange
        Long userId = 100L;
        LocalDateTime now = LocalDateTime.now();

        List<Message> messages = new ArrayList<>();
        messages.add(createTestMessage(3L, userId, 200L, "197001011234", "Latest", now));
        messages.add(createTestMessage(2L, userId, 300L, "198001011234", "Middle", now.minusHours(1)));
        messages.add(createTestMessage(1L, userId, 400L, "199001011234", "Oldest", now.minusHours(2)));

        when(messageRepository.findByFromUserIdOrderBySentAtDesc(userId))
                .thenReturn(messages);

        // Act
        List<Message> result = messageService.fromUser(userId);

        // Assert
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getContent()).isEqualTo("Latest");
        assertThat(result.get(1).getContent()).isEqualTo("Middle");
        assertThat(result.get(2).getContent()).isEqualTo("Oldest");
    }

    @Test
    void fromUser_shouldHandleNullUserId() {
        // Arrange
        when(messageRepository.findByFromUserIdOrderBySentAtDesc(null))
                .thenReturn(new ArrayList<>());

        // Act
        List<Message> result = messageService.fromUser(null);

        // Assert
        assertThat(result).isEmpty();
        verify(messageRepository).findByFromUserIdOrderBySentAtDesc(null);
    }

    @Test
    void fromUser_shouldHandleMessagesToMultipleRecipients() {
        // Arrange
        Long userId = 100L;
        List<Message> messages = new ArrayList<>();
        messages.add(createTestMessage(1L, userId, 200L, "197001011234", "To user 200", testTime));
        messages.add(createTestMessage(2L, userId, 300L, "198001011234", "To user 300", testTime.minusMinutes(5)));
        messages.add(createTestMessage(3L, userId, 400L, "199001011234", "To user 400", testTime.minusMinutes(10)));

        when(messageRepository.findByFromUserIdOrderBySentAtDesc(userId))
                .thenReturn(messages);

        // Act
        List<Message> result = messageService.fromUser(userId);

        // Assert
        assertThat(result).hasSize(3);
        assertThat(result.stream().map(Message::getToUserId))
                .containsExactly(200L, 300L, 400L);
    }

    // toUser() TESTS

    @Test
    void toUser_shouldReturnMessages_whenMessagesExist() {
        // Arrange
        Long userId = 200L;
        List<Message> expectedMessages = new ArrayList<>();
        expectedMessages.add(createTestMessage(1L, 100L, userId, "197001011234", "Message 1", testTime));
        expectedMessages.add(createTestMessage(2L, 300L, userId, "198001011234", "Message 2", testTime.minusHours(1)));

        when(messageRepository.findByToUserIdOrderBySentAtDesc(userId))
                .thenReturn(expectedMessages);

        // Act
        List<Message> result = messageService.toUser(userId);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getToUserId()).isEqualTo(userId);
        assertThat(result.get(1).getToUserId()).isEqualTo(userId);

        verify(messageRepository).findByToUserIdOrderBySentAtDesc(userId);
    }

    @Test
    void toUser_shouldReturnEmptyList_whenNoMessagesExist() {
        // Arrange
        Long userId = 999L;

        when(messageRepository.findByToUserIdOrderBySentAtDesc(userId))
                .thenReturn(new ArrayList<>());

        // Act
        List<Message> result = messageService.toUser(userId);

        // Assert
        assertThat(result).isEmpty();
        verify(messageRepository).findByToUserIdOrderBySentAtDesc(userId);
    }

    @Test
    void toUser_shouldReturnMessagesInDescendingOrder() {
        // Arrange
        Long userId = 200L;
        LocalDateTime now = LocalDateTime.now();

        List<Message> messages = new ArrayList<>();
        messages.add(createTestMessage(3L, 100L, userId, "197001011234", "Latest", now));
        messages.add(createTestMessage(2L, 300L, userId, "198001011234", "Middle", now.minusHours(1)));
        messages.add(createTestMessage(1L, 400L, userId, "199001011234", "Oldest", now.minusHours(2)));

        when(messageRepository.findByToUserIdOrderBySentAtDesc(userId))
                .thenReturn(messages);

        // Act
        List<Message> result = messageService.toUser(userId);

        // Assert
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getContent()).isEqualTo("Latest");
        assertThat(result.get(1).getContent()).isEqualTo("Middle");
        assertThat(result.get(2).getContent()).isEqualTo("Oldest");
    }

    @Test
    void toUser_shouldHandleNullUserId() {
        // Arrange
        when(messageRepository.findByToUserIdOrderBySentAtDesc(null))
                .thenReturn(new ArrayList<>());

        // Act
        List<Message> result = messageService.toUser(null);

        // Assert
        assertThat(result).isEmpty();
        verify(messageRepository).findByToUserIdOrderBySentAtDesc(null);
    }

    @Test
    void toUser_shouldHandleMessagesFromMultipleSenders() {
        // Arrange
        Long userId = 200L;
        List<Message> messages = new ArrayList<>();
        messages.add(createTestMessage(1L, 100L, userId, "197001011234", "From user 100", testTime));
        messages.add(createTestMessage(2L, 300L, userId, "198001011234", "From user 300", testTime.minusMinutes(5)));
        messages.add(createTestMessage(3L, 400L, userId, "199001011234", "From user 400", testTime.minusMinutes(10)));

        when(messageRepository.findByToUserIdOrderBySentAtDesc(userId))
                .thenReturn(messages);

        // Act
        List<Message> result = messageService.toUser(userId);

        // Assert
        assertThat(result).hasSize(3);
        assertThat(result.stream().map(Message::getFromUserId))
                .containsExactly(100L, 300L, 400L);
    }

    // INTEGRATION TESTS (testing method interactions)

    @Test
    void send_shouldAllowRetrievalByFromUser() {
        // Arrange
        Long fromUserId = 100L;
        Message messageToSend = createTestMessage(null, fromUserId, 200L, "197001011234",
                "Test message", testTime);
        Message savedMessage = createTestMessage(1L, fromUserId, 200L, "197001011234",
                "Test message", testTime);

        when(messageRepository.save(messageToSend)).thenReturn(savedMessage);
        when(messageRepository.findByFromUserIdOrderBySentAtDesc(fromUserId))
                .thenReturn(List.of(savedMessage));

        // Act
        Message sent = messageService.send(messageToSend);
        List<Message> fromUserMessages = messageService.fromUser(fromUserId);

        // Assert
        assertThat(sent.getId()).isEqualTo(1L);
        assertThat(fromUserMessages).hasSize(1);
        assertThat(fromUserMessages.get(0).getId()).isEqualTo(sent.getId());
    }

    @Test
    void send_shouldAllowRetrievalByToUser() {
        // Arrange
        Long toUserId = 200L;
        Message messageToSend = createTestMessage(null, 100L, toUserId, "197001011234",
                "Test message", testTime);
        Message savedMessage = createTestMessage(1L, 100L, toUserId, "197001011234",
                "Test message", testTime);

        when(messageRepository.save(messageToSend)).thenReturn(savedMessage);
        when(messageRepository.findByToUserIdOrderBySentAtDesc(toUserId))
                .thenReturn(List.of(savedMessage));

        // Act
        Message sent = messageService.send(messageToSend);
        List<Message> toUserMessages = messageService.toUser(toUserId);

        // Assert
        assertThat(sent.getId()).isEqualTo(1L);
        assertThat(toUserMessages).hasSize(1);
        assertThat(toUserMessages.get(0).getId()).isEqualTo(sent.getId());
    }

    @Test
    void send_shouldAllowRetrievalByPatient() {
        // Arrange
        String patientPersonnummer = "197001011234";
        Message messageToSend = createTestMessage(null, 100L, 200L, patientPersonnummer,
                "Test message", testTime);
        Message savedMessage = createTestMessage(1L, 100L, 200L, patientPersonnummer,
                "Test message", testTime);

        when(messageRepository.save(messageToSend)).thenReturn(savedMessage);
        when(messageRepository.findByPatientPersonnummerOrderBySentAtDesc(patientPersonnummer))
                .thenReturn(List.of(savedMessage));

        // Act
        Message sent = messageService.send(messageToSend);
        List<Message> patientMessages = messageService.forPatient(patientPersonnummer);

        // Assert
        assertThat(sent.getId()).isEqualTo(1L);
        assertThat(patientMessages).hasSize(1);
        assertThat(patientMessages.get(0).getId()).isEqualTo(sent.getId());
    }

    // EDGE CASES

    @Test
    void send_shouldHandleMaxLengthContent() {
        // Arrange - Column length is 4000
        String maxContent = "X".repeat(4000);
        Message messageWithMaxContent = createTestMessage(null, 100L, 200L, "197001011234",
                maxContent, testTime);
        Message savedMessage = createTestMessage(1L, 100L, 200L, "197001011234",
                maxContent, testTime);

        when(messageRepository.save(messageWithMaxContent)).thenReturn(savedMessage);

        // Act
        Message result = messageService.send(messageWithMaxContent);

        // Assert
        assertThat(result.getContent()).hasSize(4000);
        verify(messageRepository).save(messageWithMaxContent);
    }

    @Test
    void forPatient_shouldHandleLargeNumberOfMessages() {
        // Arrange
        String patientPersonnummer = "197001011234";
        List<Message> manyMessages = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            manyMessages.add(createTestMessage((long) i, 100L, 200L, patientPersonnummer,
                    "Message " + i, testTime.minusHours(i)));
        }

        when(messageRepository.findByPatientPersonnummerOrderBySentAtDesc(patientPersonnummer))
                .thenReturn(manyMessages);

        // Act
        List<Message> result = messageService.forPatient(patientPersonnummer);

        // Assert
        assertThat(result).hasSize(100);
    }

    @Test
    void send_shouldHandleWhitespaceOnlyContent() {
        // Arrange
        String whitespaceContent = "   \n\t   ";
        Message messageWithWhitespace = createTestMessage(null, 100L, 200L, "197001011234",
                whitespaceContent, testTime);
        Message savedMessage = createTestMessage(1L, 100L, 200L, "197001011234",
                whitespaceContent, testTime);

        when(messageRepository.save(messageWithWhitespace)).thenReturn(savedMessage);

        // Act
        Message result = messageService.send(messageWithWhitespace);

        // Assert
        assertThat(result.getContent()).isEqualTo(whitespaceContent);
        verify(messageRepository).save(messageWithWhitespace);
    }

    // HELPER METHODS

    private Message createTestMessage(Long id, Long fromUserId, Long toUserId,
                                      String patientPersonnummer, String content,
                                      LocalDateTime sentAt) {
        Message message = new Message();
        message.setId(id);
        message.setFromUserId(fromUserId);
        message.setToUserId(toUserId);
        message.setPatientPersonnummer(patientPersonnummer);
        message.setContent(content);
        message.setSentAt(sentAt);
        return message;
    }
}