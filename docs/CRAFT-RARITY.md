# Material rarity in Build / Continue

Verified against the pinned server's AdvancedCreationEntry.cont/run,
SimpleCreationEntry.run and CreationMatrix.getCreationEntry. No server classes
are distributed. Keybinder's action-rarity distribution supplies the roll model;
creation-specific transfer rules are implemented in CraftRarityRules.

| Action | Material rarity transfer on success |
| --- | --- |
| Continue | A component rarer than the unfinished item has a separate 1/N roll. N is the full recipe's getTotalNumberOfItems, including starters, never the remaining quantity. |
| Initial advanced creation | Eligible depleted source/target ingredients each receive a 1/N roll. The advanced constructor defaults to source depletion when both factory depletion flags are false. |
| Initial fishing-pole upgrade | A depleted source fishing pole (template 1344) carries its rarity with probability 1, overriding the ordinary source 1/N roll. |
| Simple assembly | A fully consumed weapon head, tool part or blade copies its higher rarity. Eligibility comes from that component's registered creation category. Ordinary raw materials do not receive this guaranteed copy. |

Continue does not require destruction of the entire component pile for its
rarity roll. A partially consumed rare pile retains its rarity and can supply
more rolls on later successful steps. The unfinished item keeps its current
rarity; an equal/lower rarity component cannot downgrade it or promote it to a
higher tier by addition. The final item receives the unfinished item's rarity.

Material transfer works without drumroll. The action-rarity transfer is a
separate roll. For a requested tier, independent eligible sources combine as
1 - (1 - actionChance) * (1 - sourceChance) * (1 - targetChance).
An audible drumroll conditions the action distribution on nonzero rarity.
The screen reports at least Rare / at least Supreme, conditional on a successful
step; account-status uncertainty produces the displayed Supreme range.

For the standard wagon N = 41: a rare component gives 1/41 = 2.44% material
transfer on each successful Continue, even when only one component remains.
With a rare component and an audible drumroll, at-least-Rare probability is
1 - (40/41)^2 = 4.82%, rather than adding the two percentages directly.
Across k successful rare-component steps, material-only inheritance is
1 - (40/41)^k, provided the target is still normal and the eligible components
remain rare.

The green pair determines the next-step estimate. Eligible rare inventory in
the local store marks the opportunity in muted gold; selecting it makes the
affected rarity row bright gold and recomputes the value. A rare component that
is underweight, cold when glowing is required, or conflicts with the desired
final material is not counted as a usable rarity contribution. Hover reports
material-only inheritance without drumroll and explains the current selection.

Simple-assembly inheritance and the fishing-pole exception are separate from
Improve's last-grams 1% mechanism. That Improve-only transfer rule is not applied
to Build/Continue.
