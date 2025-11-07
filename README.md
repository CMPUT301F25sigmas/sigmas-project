1.Created a notification java class to define strucutre of notification with some getters and setters

2.that works with a notification repository which handles sending notifications and keeping a log for the admin to look at
need to link that with admin.java.

3. Made a notification listener java class which i have placed in entrant dashboard and organiser dashboard, however logcat does not show it is running
that notification listner has built in methods to deactivate if user opts out (need to add opt-out code, will pull profile activity from dev as that is where i think it belongs)

4. Organiser dashboard activity has placeholder functions to send out notifications following user stories, these have to be changed.

5. User reposityor has methods to check if noptifications are enabled and update firebase accordingly, this what the opt-out code i mentioned should be in profile activity calls.

6. Firebase rules for notifications will need to be changed.

7. Need to check my firebase connectivity

8. Need to make an organiser base page for further testing, 


**ACKNOWLDEMENT THIS PROJECT USES A THIRD PARTY TOOL CALLED GLIDE-TECH BUMP PLEASE REFER TO THE LICENSE AND PLEASE REFER TO THE LINK BELOW:**
https://github.com/bumptech/glide.git
