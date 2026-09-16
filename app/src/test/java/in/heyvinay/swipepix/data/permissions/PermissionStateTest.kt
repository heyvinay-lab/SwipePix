package `in`.heyvinay.swipepix.data.permissions

import android.Manifest
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionStateTest {

    @Test
    fun getRequiredPermissions_containsImagesAndVideo() {
        val permissions = PermissionState.getRequiredPermissions()
        assertTrue(permissions.contains(Manifest.permission.READ_MEDIA_IMAGES))
        assertTrue(permissions.contains(Manifest.permission.READ_MEDIA_VIDEO))
    }

    @Test
    fun albumMediaCount_computesAndStoresAccurately() {
        val count = `in`.heyvinay.swipepix.data.model.AlbumMediaCount(
            totalCount = 42,
            photoCount = 30,
            videoCount = 12,
        )
        org.junit.Assert.assertEquals(42, count.totalCount)
        org.junit.Assert.assertEquals(30, count.photoCount)
        org.junit.Assert.assertEquals(12, count.videoCount)
    }

    @Test
    fun permissionStates_distinctAndCorrect() {
        assertTrue(PermissionState.Unknown is PermissionState)
        assertTrue(PermissionState.Checking is PermissionState)
        assertTrue(PermissionState.Granted is PermissionState)
        assertTrue(PermissionState.Partial is PermissionState)
        assertTrue(PermissionState.Denied is PermissionState)
        assertTrue(PermissionState.PermissionRequired is PermissionState)

        assertTrue(PermissionState.Granted.isGrantedOrPartial)
        assertTrue(PermissionState.Partial.isGrantedOrPartial)
        org.junit.Assert.assertFalse(PermissionState.Denied.isGrantedOrPartial)
        org.junit.Assert.assertFalse(PermissionState.PermissionRequired.isGrantedOrPartial)
        org.junit.Assert.assertFalse(PermissionState.Unknown.isGrantedOrPartial)

        assertTrue(PermissionState.Denied.isDeniedOrRequired)
        assertTrue(PermissionState.PermissionRequired.isDeniedOrRequired)
        org.junit.Assert.assertFalse(PermissionState.Granted.isDeniedOrRequired)
        org.junit.Assert.assertFalse(PermissionState.Partial.isDeniedOrRequired)
    }
}
