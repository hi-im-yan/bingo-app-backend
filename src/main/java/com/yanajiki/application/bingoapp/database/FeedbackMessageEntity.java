package com.yanajiki.application.bingoapp.database;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * JPA entity representing a feedback message submitted via the contact form.
 * <p>
 * Maps to the {@code feedback_messages} table. The {@code email} and {@code phone}
 * fields are optional (nullable). Use the static factory method {@link #create} to
 * construct instances — do not use the constructor directly in application code.
 * </p>
 */
@Entity
@Table(name = "feedback_messages")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class FeedbackMessageEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** The sender's name. Required. */
	@Column(nullable = false, length = 100)
	private String name;

	/** The sender's email address. Optional. */
	@Column(length = 254)
	private String email;

	/** The sender's phone number. Optional. */
	@Column(length = 20)
	private String phone;

	/** The feedback message body. Required. */
	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	/** Timestamp set automatically by Hibernate on insert. */
	@CreationTimestamp
	private Instant createdAt;

	/**
	 * Static factory method for constructing a new {@link FeedbackMessageEntity}.
	 *
	 * @param name    the sender's name (required)
	 * @param email   the sender's email address (nullable)
	 * @param phone   the sender's phone number (nullable)
	 * @param content the feedback message body (required)
	 * @return a new entity instance ready for persistence
	 */
	public static FeedbackMessageEntity create(String name, String email, String phone, String content) {
		FeedbackMessageEntity entity = new FeedbackMessageEntity();
		entity.setName(name);
		entity.setEmail(email);
		entity.setPhone(phone);
		entity.setContent(content);
		return entity;
	}
}
