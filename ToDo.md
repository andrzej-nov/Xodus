### Simple project TODO list

- [x] Once a ball selector is clicked, do not show selectors for that ball until a tile is placed to the field
- [x] Add the tile-replacement moves
- [ ] Add the Chaos tile moves (0, 1, 2, controlled by settings). Chaos adds its tiles after the player tile, and only then balls advance.
- [ ] After placing a tile, advance balls, kill collided ones and replan tracks
  ball.forEach { b -> advanceToNextTile(b) }
  planTracks()
- [ ] Add the "Die/Reincarnate" settings switch
- [ ] Process reincarnation on track forks
- [ ] End game when no balls left
- [ ] Add the Shredder line (controlled by settings)
- [ ] Add the player move suggestion
- [ ] Show "magnifier glass" when pressing a selector, to easier selector picking
- [ ] Add blots
- [ ] Add ball eyes
- [ ] Add animations
- [ ] Add game save/load
- [ ] Add settings to the Home screen