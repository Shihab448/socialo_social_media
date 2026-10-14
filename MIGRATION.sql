-- ============================================================
-- SOCIALO — COMPLETE SAFE SQL MIGRATION
-- Run this in your Supabase SQL Editor
-- SAFE: No DROP TABLE, no DELETE, no data loss
-- ============================================================

-- ============================================================
-- A. SCHEMA CHANGES (run in Supabase SQL Editor)
-- ============================================================

-- 1. Add role column to users (replaces isAdmin boolean)
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS role VARCHAR(20) NOT NULL DEFAULT 'USER';

-- Migrate existing isAdmin=true users to ADMIN role
UPDATE users SET role = 'ADMIN' WHERE is_admin = TRUE AND role = 'USER';

-- 2. Add profileLocked column (Part 10)
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS profile_locked BOOLEAN NOT NULL DEFAULT FALSE;

-- 3. Add parent_comment_id to comments (Part 9 — replies)
ALTER TABLE comments
    ADD COLUMN IF NOT EXISTS parent_comment_id BIGINT
    REFERENCES comments(id) ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_comments_parent ON comments(parent_comment_id);

-- 4. Ensure reactions UNIQUE constraint exists (Part 8)
-- (Spring Boot already created this via @UniqueConstraint but add explicitly if needed)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name='reactions' AND constraint_name='reactions_post_id_user_id_key'
    ) THEN
        ALTER TABLE reactions ADD CONSTRAINT reactions_post_id_user_id_key
            UNIQUE (post_id, user_id);
    END IF;
END $$;

-- 5. Add reaction_type column if missing
ALTER TABLE reactions
    ADD COLUMN IF NOT EXISTS reaction_type VARCHAR(20) NOT NULL DEFAULT 'LIKE';

-- 6. Indexes for performance
CREATE INDEX IF NOT EXISTS idx_users_role       ON users(role);
CREATE INDEX IF NOT EXISTS idx_reactions_post   ON reactions(post_id);
CREATE INDEX IF NOT EXISTS idx_reactions_user   ON reactions(user_id);
CREATE INDEX IF NOT EXISTS idx_comments_post    ON comments(post_id);
CREATE INDEX IF NOT EXISTS idx_comments_user    ON comments(user_id);

-- ============================================================
-- B. RLS POLICIES (run in Supabase SQL Editor)
-- ============================================================
-- NOTE: These are Supabase RLS policies.
-- This app uses Spring Boot session auth, NOT Supabase JWT.
-- For a Spring Boot app accessing Supabase via JDBC (not REST API),
-- RLS does NOT apply to server-side JDBC connections
-- (they bypass RLS by default as postgres superuser).
--
-- RLS only matters if you expose Supabase REST/GraphQL directly
-- to the browser (e.g. via supabase-js client).
--
-- Since this app uses Spring Boot JDBC:
--   → Security is enforced in Java service layer
--   → Role checks: UserService.promoteRole() — SUPER_ADMIN only
--   → Profile access: ProfileController.userProfile() — friend check
--   → Reaction ownership: ReactionService — user can only change own
--   → Comment ownership: CommentService.deleteComment() — user check
--
-- If you later add a Supabase JS client, add RLS policies below:

-- Enable RLS on tables (optional — only needed for direct Supabase client access)
-- ALTER TABLE users      ENABLE ROW LEVEL SECURITY;
-- ALTER TABLE posts      ENABLE ROW LEVEL SECURITY;
-- ALTER TABLE comments   ENABLE ROW LEVEL SECURITY;
-- ALTER TABLE reactions  ENABLE ROW LEVEL SECURITY;

-- Example RLS policies (only needed if using supabase-js directly):
-- CREATE POLICY "users can update own profile"
--   ON users FOR UPDATE USING (auth.uid()::text = id::text);
-- CREATE POLICY "users can manage own reactions"
--   ON reactions FOR ALL USING (auth.uid()::text = user_id::text);
-- CREATE POLICY "users can manage own comments"
--   ON comments FOR DELETE USING (auth.uid()::text = user_id::text);

-- ============================================================
-- C. FIRST SUPER_ADMIN SETUP
-- ============================================================
-- Step 1: Create a normal account via http://localhost:8081/signup
-- Step 2: Run this query (replace with your actual email):

-- UPDATE users
-- SET role = 'SUPER_ADMIN'
-- WHERE email = 'your-email@example.com';

-- Step 3: Login at http://localhost:8081/admin/login
-- Step 4: You now have full SUPER_ADMIN access

-- ============================================================
-- D. VERIFY MIGRATION
-- ============================================================

-- Check role column exists:
-- SELECT id, first_name, last_name, email, role FROM users LIMIT 10;

-- Check parent_comment_id exists:
-- SELECT id, content, parent_comment_id FROM comments LIMIT 10;

-- Check reactions have reaction_type:
-- SELECT id, reaction_type FROM reactions LIMIT 10;

-- Check profile_locked:
-- SELECT id, first_name, profile_locked FROM users LIMIT 10;

-- ============================================================
-- E. SUMMARY OF CHANGES
-- ============================================================
-- users table:
--   + role VARCHAR(20) DEFAULT 'USER'   (USER/MODERATOR/ADMIN/SUPER_ADMIN)
--   + profile_locked BOOLEAN DEFAULT FALSE
--
-- comments table:
--   + parent_comment_id BIGINT FK → comments(id)
--
-- reactions table:
--   + UNIQUE(post_id, user_id) constraint confirmed
--   + reaction_type VARCHAR(20) DEFAULT 'LIKE'
--
-- Indexes added:
--   idx_users_role, idx_reactions_post, idx_reactions_user
--   idx_comments_post, idx_comments_user, idx_comments_parent
-- ============================================================
