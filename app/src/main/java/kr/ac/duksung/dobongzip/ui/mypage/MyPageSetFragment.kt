package kr.ac.duksung.dobongzip.ui.mypage

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import kr.ac.duksung.dobongzip.R
import kr.ac.duksung.dobongzip.databinding.FragmentMyPageBinding

class MyPageSetFragment : Fragment() {

    private var _binding: FragmentMyPageBinding? = null
    private val binding get() = _binding!!

    // 1) AndroidX Photo Picker 등록: 이미지 1장
    private val pickImage =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            if (uri != null) {
                // 2) 즉시 미리보기 반영 (Glide 권장)
                Glide.with(this)
                    .load(uri)
                    .centerCrop()
                    .placeholder(R.drawable.prf3)
                    .into(binding.profileImage)

                // 3) (선택) 서버 업로드용으로 ViewModel/Repository에 전달하거나
                //    앱 캐시에 복사한 후 업로드 로직을 부르세요.
                // uploadProfileImage(uri)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyPageBinding.inflate(inflater, container, false)

        // ✅ "개인정보 수정" 버튼 → 수정 화면 이동
        binding.myPageButton.setOnClickListener {
            findNavController().navigate(R.id.myPageEditFragment)
        }

        // ✅ 프로필 사진/텍스트 클릭 → 사진 선택
        binding.editProfileText.setOnClickListener { openPhotoPicker() }
        binding.profileImage.setOnClickListener { openPhotoPicker() }

        // (선택) 뒤로가기 버튼
        binding.backButton.setOnClickListener { findNavController().popBackStack() }

        return binding.root
    }

    private fun openPhotoPicker() {
        // 이미지만 선택하도록 필터링
        pickImage.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
