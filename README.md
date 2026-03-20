<hr>
<h1 style="text-align: center;"><span style="text-decoration: underline;">What is Interactive Fluids?</span></h1>
<hr>
<p style="text-align: center;">This&nbsp;<strong>library</strong> adds a new ticker for fluids, which converts the <strong>surrounding blocks</strong> regarding the type of fluid into a <strong>new set of blocks</strong>. Currently, it doesn't do much on its own and needs packs that implement its logic in Hytale.</p>
<p style="text-align: center;">&nbsp;</p>
<hr>
<h1 style="text-align: center;"><span style="text-decoration: underline;">How does Interactive Fluids work?</span></h1>
<hr>
<p style="text-align: center;">This ticker is pretty similar to the <strong>Default Ticker</strong> from vanilla Hytale.</p>
<p style="text-align: center;"><img src="https://media.forgecdn.net/attachments/description/1464378/description_fce52dd6-0ad0-4cda-92c4-9f84905164e8.png"></p>
<p style="text-align: center;">&nbsp;</p>
<hr>
<h1 style="text-align: center;"><span style="text-decoration: underline;">Flow Shapes</span></h1>
<hr>
<p style="text-align: center;">This <strong>option </strong>can modify the shape/range of the modified blocks based on their interaction with the fluid that implements the Interactive Fluid Ticker. The shape acts as a way to control the range of changed blocks by this ticker.</p>
<p style="text-align: center;">By <strong>default</strong> it uses the diamond shape with the size of 1, which is also equal to the implementation before v.0.3.0.</p>
<p style="text-align: center;"><strong>Currently supported shapes: </strong></p>
<p style="text-align: center;"><strong>Cube</strong>(size), <strong>Cuboid</strong>(width, height, depth), <strong>Sphere</strong>(radius), <strong>Diamond</strong>(radius), <strong>Flat_Square</strong>(size), <strong>Flat_Rectangle</strong>(width, height), <strong>Flat_Circle</strong>(radius), <strong>Flat_Diamond</strong>(radius)</p>
<p style="text-align: center;">➔ The arguments mentioned in the <strong>brackets</strong> must be defined in the Shape Size Options Config</p>
<h3 style="text-align: center;"><strong><span style="text-decoration: underline;">Example:</span></strong></h3>
<p style="text-align: center;"><img src="https://media.forgecdn.net/attachments/description/1464378/description_212ee0ce-2d3b-4041-8c0d-c98852cad513.png"></p>
<p style="text-align: center;">➔ argument 0 = width of cuboid</p>
<p style="text-align: center;">➔ argument 1 = height of cuboid</p>
<p style="text-align: center;">➔ argument 2 = depth of cuboid</p>
<p style="text-align: center;">&nbsp;</p>
<hr>
<h1 style="text-align: center;"><span style="text-decoration: underline;">Fluid Collisions</span></h1>
<hr>
<p style="text-align: center;">This <strong>option </strong>works exactly like the <strong>Collisions </strong>option in the <strong>Default Ticker</strong>.</p>
<p style="text-align: center;"><br>Here an <strong>example </strong>for a fluid collision of <strong>water </strong>with <strong>lava</strong>.</p>
<p style="text-align: center;">Lava gets converted either to <strong>cobblestone </strong>or cooled <strong>magma</strong>, regarding if it is a <strong>lava source block or not</strong>.</p>
<p style="text-align: center;"><img src="https://media.forgecdn.net/attachments/description/1464378/description_cca4e7fd-4327-4537-9900-0eb63af3f732.png"></p>
<p style="text-align: center;">&nbsp;</p>
<hr>
<h1 style="text-align: center;"><span style="text-decoration: underline;">Block Collisions</span></h1>
<hr>
<p style="text-align: center;">The <strong>Block Collisions</strong> list is a new list that makes it possible to add blocks that get converted into a new set of blocks. It is separated into two different types of block collision <strong>phases</strong>.&nbsp;</p>
<p style="text-align: center;">&nbsp;</p>
<p><img style="display: block; margin-left: auto; margin-right: auto;" src="https://media.forgecdn.net/attachments/description/1464378/description_cee3cadb-a587-4a45-91f3-504e86875de1.png"></p>
<p style="text-align: center;">If you define a block collision in the&nbsp;<strong>Spread</strong> list, it starts the block collision on the spread of the fluid.</p>
<p style="text-align: center;">If you define a block collision in the&nbsp;<strong>Demote</strong> list, it starts the block collision when the fluid has completely finished demoting.</p>
<p style="text-align: center;">&nbsp;</p>
<p><img style="display: block; margin-left: auto; margin-right: auto;" src="https://media.forgecdn.net/attachments/description/1464378/description_5fa6819d-dbf9-4994-843c-5aa4a9412c5b.png"></p>
<p style="text-align: center;">The elements in the <strong>Spread </strong>list don't do anything <strong>(since v.0.7.0)</strong> and can be used to comment on specific conversions.</p>
<p style="text-align: center;">The same applies to the <strong>Demote</strong> list.</p>
<p style="text-align: center;">&nbsp;</p>
<p style="text-align: center;"><img src="https://media.forgecdn.net/attachments/description/1464378/description_b128ed46-6648-4767-84a7-3fca6a066625.png"></p>
<p style="text-align: center;">The element is split in <strong>Condition</strong> &amp; <strong>Result</strong> config.</p>
<p style="text-align: center;">The <strong>Condition </strong>defines what specific properties the block has to fulfill in order to get converted into an over block.</p>
<p style="text-align: center;">The block that will be placed if the condition is fulfilled is defined in the <strong>Result</strong> config. You can also define a <strong>sound</strong> that gets played on conversion.</p>
<p style="text-align: center;">&nbsp;</p>
<hr>
<h1 style="text-align: center;"><span style="text-decoration: underline;">Preview</span></h1>
<hr>
<p style="text-align: center;">The Water turns dirt into mud ➔ implemented in the pack "<strong><a href="https://www.curseforge.com/hytale/mods/vanillaflow" target="_blank" rel="nofollow noopener">VanillaFlow</a>"</strong></p>
<p><img style="display: block; margin-left: auto; margin-right: auto;" src="https://media.forgecdn.net/attachments/description/1464378/description_c8b878ad-4efe-4bab-8c3d-b8f56a2fb066.gif"></p>
