# Future Ideas

Ideas to explore later. Not committed to any timeline.

## Date Section Headers
Group grid items by date ("Today", "Yesterday", "March 15") with optional toggle in Filters bottom sheet ("Group by date", off by default). Tapping a date header could select all items in that group.

## Trash Access
Add a way to view trashed items directly in the app. Options: query `MediaStore.IS_TRASHED` and show in our own grid with a "Restore" button, or link to the system gallery trash. Placement TBD — could be an icon in the header bar or an option in the Filters bottom sheet.

## "Left Off Here" Divider
A horizontal divider line in the grid marking where the user stopped scrolling last session (like Slack's "new messages" line). Could complement or replace the Continue FAB. Needs to consider: we already dim viewed items with opacity, so the visual benefit may be marginal.
