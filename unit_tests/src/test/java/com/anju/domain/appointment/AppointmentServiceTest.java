package com.anju.domain.appointment;

import com.anju.common.BusinessException;
import com.anju.domain.auth.CurrentUserService;
import com.anju.domain.auth.User;
import com.anju.domain.appointment.dto.RescheduleAppointmentRequest;
import com.anju.domain.appointment.dto.CreateAppointmentRequest;
import com.anju.domain.property.PropertyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private AppointmentService appointmentService;

    private Appointment existingAppointment;
    private User currentUser;

    @BeforeEach
    void setUp() {
        existingAppointment = new Appointment();
        existingAppointment.setId(1L);
        existingAppointment.setStaffId(10L);
        existingAppointment.setResourceId(20L);
        existingAppointment.setRescheduleCount(0);
        existingAppointment.setStatus("PENDING");

        currentUser = new User();
        currentUser.setId(10L);
        currentUser.setRole("STAFF");
        when(currentUserService.requireCurrentUser()).thenReturn(currentUser);
    }

    @Test
    void testReschedule_exceedingMaxReschedules_throwsException() {
        existingAppointment.setRescheduleCount(2); // Already 2 reschedules
        existingAppointment.setStartTime(LocalDateTime.now().plusDays(2)); // More than 24h
        existingAppointment.setEndTime(LocalDateTime.now().plusDays(2).plusHours(1));
        
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(existingAppointment));

        RescheduleAppointmentRequest request = new RescheduleAppointmentRequest();
        request.setStartTime(LocalDateTime.now().plusDays(3));
        request.setEndTime(LocalDateTime.now().plusDays(3).plusHours(1));

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            appointmentService.reschedule(1L, request);
        });

        assertEquals(4011, exception.getCode());
        assertEquals("Exceeded maximum of 2 reschedules.", exception.getMessage());
    }

    @Test
    void testReschedule_insufficientAdvanceNotice_throwsException() {
        // Less than 24 hours from now
        existingAppointment.setStartTime(LocalDateTime.now().plusHours(10));
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(existingAppointment));

        RescheduleAppointmentRequest request = new RescheduleAppointmentRequest();
        request.setStartTime(LocalDateTime.now().plusDays(3));
        request.setEndTime(LocalDateTime.now().plusDays(3).plusHours(1));

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            appointmentService.reschedule(1L, request);
        });

        assertEquals(4010, exception.getCode());
        assertTrue(exception.getMessage().contains("less than 24 hours"));
    }

    @Test
    void testReschedule_overlappingTimeSlot_throwsException() {
        existingAppointment.setStartTime(LocalDateTime.now().plusDays(2));
        existingAppointment.setEndTime(LocalDateTime.now().plusDays(2).plusHours(1));
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(existingAppointment));

        LocalDateTime newStartTime = LocalDateTime.now().plusDays(3);
        LocalDateTime newEndTime = LocalDateTime.now().plusDays(3).plusHours(1);

        RescheduleAppointmentRequest request = new RescheduleAppointmentRequest();
        request.setStartTime(newStartTime);
        request.setEndTime(newEndTime);

        // Simulate conflict
        when(appointmentRepository.existsConflict(
                eq(10L), eq(20L), eq(newStartTime), eq(newEndTime), eq("CANCELLED")
        )).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            appointmentService.reschedule(1L, request);
        });

        assertEquals(4006, exception.getCode());
        assertTrue(exception.getMessage().contains("conflicts with existing schedule"));
    }

    @Test
    void testReschedule_success() {
        existingAppointment.setStartTime(LocalDateTime.now().plusDays(2));
        existingAppointment.setEndTime(LocalDateTime.now().plusDays(2).plusHours(1));
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(existingAppointment));

        LocalDateTime newStartTime = LocalDateTime.now().plusDays(3);
        LocalDateTime newEndTime = LocalDateTime.now().plusDays(3).plusHours(1);

        RescheduleAppointmentRequest request = new RescheduleAppointmentRequest();
        request.setStartTime(newStartTime);
        request.setEndTime(newEndTime);

        when(appointmentRepository.existsConflict(
                eq(10L), eq(20L), eq(newStartTime), eq(newEndTime), eq("CANCELLED")
        )).thenReturn(false);

        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Appointment result = appointmentService.reschedule(1L, request);

        assertEquals(1, result.getRescheduleCount());
        assertEquals("RESCHEDULED", result.getStatus());
        assertEquals(newStartTime, result.getStartTime());
        verify(appointmentRepository).save(any(Appointment.class));
    }

    @Test
    void testCreate_withIdempotencyKey_returnsExistingAppointment() {
        Appointment existing = new Appointment();
        existing.setId(99L);
        existing.setStaffId(10L);
        existing.setNumber("APPT-EXISTING");

        when(appointmentRepository.findByIdempotencyKey("idem-appointment")).thenReturn(Optional.of(existing));

        CreateAppointmentRequest request = new CreateAppointmentRequest();
        request.setStaffId(10L);
        request.setResourceId(20L);
        request.setStartTime(LocalDateTime.now().plusDays(2));
        request.setEndTime(LocalDateTime.now().plusDays(2).plusMinutes(30));

        Appointment result = appointmentService.create(request, "idem-appointment");

        assertEquals(99L, result.getId());
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    void testCreate_serverControlsLifecycleFields() {
        when(propertyRepository.existsById(20L)).thenReturn(true);
        when(appointmentRepository.existsConflict(anyLong(), anyLong(), any(), any(), eq("CANCELLED"))).thenReturn(false);
        when(appointmentRepository.existsByNumber("APPT-CUSTOM")).thenReturn(false);
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateAppointmentRequest request = new CreateAppointmentRequest();
        request.setNumber("appt-custom");
        request.setStaffId(10L);
        request.setResourceId(20L);
        request.setStartTime(LocalDateTime.now().plusDays(2));
        request.setEndTime(LocalDateTime.now().plusDays(2).plusMinutes(30));
        request.setStatus("COMPLETED");
        request.setPenalty(new BigDecimal("88.88"));
        request.setRescheduleCount(2);

        Appointment result = appointmentService.create(request, null);

        assertEquals("PENDING", result.getStatus());
        assertEquals(BigDecimal.ZERO, result.getPenalty());
        assertEquals(0, result.getRescheduleCount());
    }

    @Test
    void testReschedule_releasedAppointment_throwsException() {
        existingAppointment.setStatus("RELEASED");
        existingAppointment.setStartTime(LocalDateTime.now().plusDays(2));
        existingAppointment.setEndTime(LocalDateTime.now().plusDays(2).plusHours(1));
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(existingAppointment));

        RescheduleAppointmentRequest request = new RescheduleAppointmentRequest();
        request.setStartTime(LocalDateTime.now().plusDays(3));
        request.setEndTime(LocalDateTime.now().plusDays(3).plusHours(1));

        BusinessException exception = assertThrows(BusinessException.class, () -> appointmentService.reschedule(1L, request));

        assertEquals(4013, exception.getCode());
    }
}
