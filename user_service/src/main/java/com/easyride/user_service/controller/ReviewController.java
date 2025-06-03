@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    @PostMapping("/driver-to-passenger")
    public ResponseEntity<?> createDriverReview(@RequestBody DriverReviewRequest request) {
        reviewService.createDriverReview(
                request.getDriverId(),
                request.getPassengerId(),
                request.getRating(),
                request.getTags()
        );
        return ResponseEntity.ok().build();
    }
}