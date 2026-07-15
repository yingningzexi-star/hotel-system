package com.hotel.system.service;

import com.hotel.system.entity.BookingOrder;
import com.hotel.system.entity.Review;
import com.hotel.system.entity.User;
import com.hotel.system.repository.BookingOrderRepository;
import com.hotel.system.repository.ReviewRepository;
import com.hotel.system.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private BookingOrderRepository bookingOrderRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public Review createReview(Long userId, Long roomTypeId, Long orderId, Integer rating, String comment) {
        BookingOrder order = bookingOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));

        if (!order.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("无权评价该订单");
        }

        if (!"COMPLETED".equals(order.getStatus())) {
            throw new IllegalArgumentException("只有已完成的订单才能评价");
        }

        if (reviewRepository.existsByOrderId(orderId)) {
            throw new IllegalArgumentException("该订单已评价过");
        }

        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("评分必须在1-5分之间");
        }

        User user = userRepository.findById(userId).orElseThrow();
        Review review = new Review();
        review.setUser(user);
        review.setRoomType(order.getRoomType());
        review.setOrder(order);
        review.setRating(rating);
        review.setComment(comment);
        review.setCreatedAt(LocalDateTime.now());

        return reviewRepository.save(review);
    }

    public List<Review> getRoomReviews(Long roomTypeId) {
        return reviewRepository.findByRoomTypeIdOrderByCreatedAtDesc(roomTypeId);
    }

    public List<Review> getUserReviews(Long userId) {
        return reviewRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Double getRoomAverageRating(Long roomTypeId) {
        return reviewRepository.getAverageRatingByRoomTypeId(roomTypeId);
    }

    public long getRoomReviewCount(Long roomTypeId) {
        return reviewRepository.countByRoomTypeId(roomTypeId);
    }

    public boolean hasReviewed(Long orderId) {
        return reviewRepository.existsByOrderId(orderId);
    }

    public List<Review> getAllReviews() {
        return reviewRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public void deleteReview(Long reviewId) {
        reviewRepository.deleteById(reviewId);
    }
}
